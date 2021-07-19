package com.xl.redisaux.limiter.component;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xl.redisaux.common.api.LimiteGroupConfig;
import com.xl.redisaux.common.enums.TimeUnitEnum;
import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.common.utils.qps.QpsCounter;
import com.xl.redisaux.common.utils.qps.WindowQpsCounter;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterRegistar;
import com.xl.redisaux.limiter.core.BaseRateLimiter;
import com.xl.redisaux.limiter.core.handler.GroupHandler;
import com.xl.redisaux.limiter.core.handler.GroupHandlerList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lulu
 * @Date 2020/2/15 21:15
 */

public class LimiterGroupService {


    @Autowired
    @Qualifier(LimiterConstants.LIMITER)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final GroupHandlerList groupHandlers;

    private final DefaultRedisScript getGroupScript;

    private final DefaultRedisScript delGroupScript;

    private final Map<String, QpsCounter> qpsCounterMap;
    private ConcurrentHashMap<String, String> groupIdMap;


    public LimiterGroupService() {
        groupHandlers = new GroupHandlerList();
        getGroupScript = new DefaultRedisScript();
        getGroupScript.setResultType(List.class);
        getGroupScript.setScriptText(getAllGroupScript());
        delGroupScript = new DefaultRedisScript();
        delGroupScript.setScriptText(delGroupScript());
        qpsCounterMap = new ConcurrentHashMap<>();
        groupIdMap = RedisLimiterRegistar.connectConsole.get() ? new ConcurrentHashMap() : null;
    }

    /**
     * limiterGroup规则
     *
     * @param limiteGroup
     * @param saveInRedis
     */
    public void save(LimiteGroupConfig limiteGroup, boolean saveInRedis, Boolean removeOtherLimiter) {
        if (saveInRedis) {
            String s = null;
            try {
                s = objectMapper.writeValueAsString(limiteGroup);
            } catch (JsonProcessingException e) {
                throw new RedisAuxException("json序列化异常:" + e.getMessage());
            }
            if (removeOtherLimiter) {

                Integer currentMode = limiteGroup.getCurrentMode();
                List<String> keyList = null;
                if (currentMode.equals(LimiterConstants.TOKEN_LIMITER)) {
                    keyList = Arrays.asList(
                            CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.FUNNEL)
                            , CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.WINDOW)
                    );
                }
                if (currentMode.equals(LimiterConstants.FUNNEL_LIMITER)) {
                    keyList = Arrays.asList(
                            CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.TOKEN),
                            CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.WINDOW));

                }
                if (currentMode.equals(LimiterConstants.WINDOW_LIMITER)) {
                    keyList = Arrays.asList(CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.TOKEN),
                            CommonUtil.getLimiterTypeName(limiteGroup.getId(), LimiterConstants.FUNNEL));
                }
                redisTemplate.execute(delGroupScript, keyList);
            }
            redisTemplate.opsForValue().set(CommonUtil.getLimiterConfigName(limiteGroup.getId()), s);
        }
        BaseRateLimiter.createOrUpdateGroups(limiteGroup);

    }

    /**
     * todo 设置定时任务往redis上更新，本地即时更新
     *
     * @param success
     * @param config
     */

    public void updateCount(boolean success, LimiteGroupConfig config) {
        /**
         * 本地记录
         */
        String id = config.getId();
        QpsCounter qpsCounter = qpsCounterMap.get(id);
        if (qpsCounter == null) {
            qpsCounter = new WindowQpsCounter(config.getBucketSize(), config.getCountDuring(), config.getCountDuringUnit());
        }
        qpsCounter.pass(success);
        qpsCounterMap.put(id, qpsCounter);
    }


    public Map<String, Object> getCount(String id) {
        QpsCounter qpsCounter = qpsCounterMap.get(id);
        if (Objects.isNull(qpsCounter)) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("message", "no data");
            return map;
        }
        Map<String, Object> sum = qpsCounter.getSum();
        LimiteGroupConfig limiterConfig = getLimiterConfig(id);
        sum.put("period", limiterConfig.getCountDuring());
        sum.put("unit", TimeUnitEnum.getMode(limiterConfig.getCountDuringUnit()).toString());
        return sum;
    }


    public void saveAll(List<LimiteGroupConfig> limiteGroups, boolean saveInRedis) {
        if (saveInRedis) {
            Map<String, String> resMap = new HashMap<>();
            for (LimiteGroupConfig limiteGroup : limiteGroups) {
                try {
                    resMap.put(CommonUtil.getLimiterConfigName(limiteGroup.getId()), objectMapper.writeValueAsString(limiteGroup));
                } catch (JsonProcessingException e) {
                    throw new RedisAuxException("json序列化异常:" + e.getMessage());
                }
            }
            redisTemplate.opsForValue().multiSet(resMap);
        }
        BaseRateLimiter.createOrUpdateGroups(limiteGroups);

    }

    public LimiteGroupConfig getLimiterConfig(String groupId) {
        addGroupIdWhenExecute(groupId);
        LimiteGroupConfig group = BaseRateLimiter.rateLimitGroupConfigMap.get(groupId);
        if (group == null) {
            reload(groupId);
        }
        return BaseRateLimiter.rateLimitGroupConfigMap.get(groupId);
    }

    public List<LimiteGroupConfig> getConfigByGroupIds(Set<String> groupIds) {
        List<LimiteGroupConfig> res = new ArrayList<>(groupIds.size());
        Set<Map.Entry<String, LimiteGroupConfig>> entries = BaseRateLimiter.rateLimitGroupConfigMap.entrySet();
        for (Map.Entry<String, LimiteGroupConfig> entry : entries) {
            if (groupIds.contains(entry.getKey())) {
                res.add(entry.getValue());
            }
        }
        return res;
    }

    public void reload(String groupId) {
        String configStr = redisTemplate.opsForValue().get(CommonUtil.getLimiterConfigName(groupId)).toString();
        try {
            if (!StringUtils.isEmpty(configStr)) {
                BaseRateLimiter.rateLimitGroupConfigMap.put(groupId, objectMapper.readValue(configStr, LimiteGroupConfig.class));
            }
        } catch (JsonProcessingException e) {
            throw new RedisAuxException("json序列化异常:" + e.getMessage());
        }
    }

    public void clear(String groupId) {
        BaseRateLimiter.rateLimitGroupConfigMap.remove(groupId);
        redisTemplate.execute(delGroupScript, Collections.singletonList(CommonUtil.getLimiterConfigName(groupId) + "*"));
    }

    public List<LimiteGroupConfig> getAll() {
        List<LimiteGroupConfig> list = new LinkedList<>();
        if (BaseRateLimiter.rateLimitGroupConfigMap.size() == 0) {
            List<String> res = (List<String>) redisTemplate.execute(getGroupScript, null);

            List<LimiteGroupConfig> limiteGroups = new LinkedList<>();
            if (res != null) {
                for (String s : res) {
                    try {
                        limiteGroups.add(objectMapper.readValue(s, LimiteGroupConfig.class));
                    } catch (JsonProcessingException e) {
                        throw new RedisAuxException("json序列化异常:" + e.getMessage());
                    }
                }
            }
        } else {
            for (LimiteGroupConfig value : BaseRateLimiter.rateLimitGroupConfigMap.values()) {
                list.add(value);
            }
        }
        return list;

    }

    private void addGroupIdWhenExecute(String groupId) {
        if (RedisLimiterRegistar.connectConsole.get()) {
            this.groupIdMap.put(groupId, "");
        }
    }

    public Set<String> getGroupIds() {
        return groupIdMap == null ? new HashSet() : groupIdMap.keySet();
    }

    /**
     * 添加拦截器
     *
     * @param groupHandler
     */
    public LimiterGroupService addHandler(GroupHandler groupHandler) {
        this.groupHandlers.add(groupHandler);
        return this;
    }

    public LimiterGroupService removeHandler(GroupHandler groupHandler) {
        this.groupHandlers.remove(groupHandler);
        return this;
    }


    public int handle(LimiteGroupConfig limitGroupConfig, String ip,
                      String url, BaseRateLimiter baseRateLimiter,
                      String methodKey) {
        int handle = LimiterConstants.PASS;
        if (groupHandlers.isChange()) {
            groupHandlers.sort();
        }
        for (GroupHandler groupHandler : groupHandlers) {
            handle = groupHandler.handle(limitGroupConfig, ip, url, baseRateLimiter, methodKey);
            if (handle != LimiterConstants.CONTINUE) {
                break;
            }
        }
        return handle;
    }


    private String getAllGroupScript() {
        StringBuilder builder = new StringBuilder();
        builder.append("local array = {}\n")
                .append("local others = redis.call('keys', 'limiter-*')\n")
                .append("for i = 1, table.getn(others) do\n")
                .append("    array[i] = redis.call('get', others[i])\n")
                .append("end\nreturn array");
        return builder.toString();
    }

    private String delGroupScript() {
        StringBuilder builder = new StringBuilder();
        builder.append("for i = 1,table.getn(KEYS) do\n")
                .append("    local array=redis.call('keys',KEYS[i])\n")
                .append("    for k = 1,table.getn(array) do\n")
                .append("        redis.call('del',array[k])\n")
                .append("    end\nend");
        return builder.toString();
    }
}
