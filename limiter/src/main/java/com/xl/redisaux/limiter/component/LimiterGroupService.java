package com.xl.redisaux.limiter.component;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xl.redisaux.common.api.LimitGroupConfig;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lulu
 * @Date 2020/2/15 21:15
 */
public class LimiterGroupService {


    @Resource(name=LimiterConstants.LIMITER)
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    private final GroupHandlerList groupHandlers;

    private final DefaultRedisScript getGroupScript;

    private final DefaultRedisScript delGroupScript;

    private final Map<String, QpsCounter> qpsCounterMap;
    private final ConcurrentHashMap<String, String> groupIdMap;


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

    public void save(LimitGroupConfig config) {
        save(config, config.isSaveInRedis(), config.isRemoveOtherLimit());
    }

    /**
     * @param limitGroup         配置类
     * @param saveInRedis
     * @param removeOtherLimiter
     */
    public void save(LimitGroupConfig limitGroup, boolean saveInRedis, Boolean removeOtherLimiter) {
        if (saveInRedis) {
            String s = null;
            try {
                s = objectMapper.writeValueAsString(limitGroup);
            } catch (JsonProcessingException e) {
                throw new RedisAuxException("json序列化异常:" + e.getMessage());
            }
            if (removeOtherLimiter) {
                removeOtherLimiter(Collections.singletonList(limitGroup));
            }
            redisTemplate.opsForValue().set(CommonUtil.getLimiterConfigName(limitGroup.getId()), s);
        }
        BaseRateLimiter.createOrUpdateGroups(limitGroup);

    }

    private void removeOtherLimiter(Collection<LimitGroupConfig> limitGroups) {
        if (limitGroups == null || limitGroups.size() == 0) {
            return;
        }
        List<String> keyList = new ArrayList<>(limitGroups.size() * 2);
        for (LimitGroupConfig limitGroup : limitGroups) {
            Integer currentMode = limitGroup.getCurrentMode();
            if (currentMode.equals(LimiterConstants.TOKEN_LIMITER)) {
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.FUNNEL));
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.WINDOW));
                continue;
            }
            if (currentMode.equals(LimiterConstants.FUNNEL_LIMITER)) {
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.TOKEN));
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.WINDOW));
                continue;
            }
            if (currentMode.equals(LimiterConstants.WINDOW_LIMITER)) {
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.TOKEN));
                keyList.add(CommonUtil.getLimiterTypeName(limitGroup.getId(), LimiterConstants.FUNNEL));
            }
        }
        redisTemplate.execute(delGroupScript, keyList);
    }

    /**
     * todo 设置定时任务往redis上更新，本地即时更新
     *
     * @param success
     * @param config
     */
    public void updateCount(boolean success, LimitGroupConfig config) {
        /**
         * 本地记录
         */
        String id = config.getId();
        QpsCounter qpsCounter = qpsCounterMap.get(id);
        if (qpsCounter == null) {
            qpsCounter = new WindowQpsCounter(config.getBucketSize(), config.getQpsCountDuring(), config.getQpsCountDuringUnit());
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
        LimitGroupConfig limiterConfig = getLimiterConfig(id);
        sum.put("period", limiterConfig.getQpsCountDuring());
        sum.put("unit", TimeUnitEnum.getMode(limiterConfig.getQpsCountDuringUnit()).toString());
        return sum;
    }


    public void saveAll(List<LimitGroupConfig> limitGroups) {
        List<LimitGroupConfig> saveInRedis = new ArrayList<>(limitGroups.size());
        List<LimitGroupConfig> removeOthers = new ArrayList<>(limitGroups.size());
        for (LimitGroupConfig limitGroup : limitGroups) {
            if (limitGroup.isSaveInRedis()) {
                saveInRedis.add(limitGroup);
                if (limitGroup.isRemoveOtherLimit()) {
                    removeOthers.add(limitGroup);
                }
            }
        }
        //只有redis才回移除数据
        if (saveInRedis.size() > 0) {
            Map<String, String> resMap = new HashMap<>(limitGroups.size());
            for (LimitGroupConfig limitGroup : limitGroups) {
                try {
                    resMap.put(CommonUtil.getLimiterConfigName(limitGroup.getId()), objectMapper.writeValueAsString(limitGroup));
                } catch (JsonProcessingException e) {
                    throw new RedisAuxException("json序列化异常:" + e.getMessage());
                }
            }
            redisTemplate.opsForValue().multiSet(resMap);
            if (removeOthers.size() > 0) {
                removeOtherLimiter(removeOthers);
            }
        }
        BaseRateLimiter.createOrUpdateGroups(limitGroups);
    }

    public LimitGroupConfig getLimiterConfig(String groupId) {
        addGroupIdWhenExecute(groupId);
        LimitGroupConfig group = BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.get(groupId);
        if (group == null) {
            reload(groupId);
        }
        return BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.get(groupId);
    }

    public List<LimitGroupConfig> getConfigByGroupIds(Set<String> groupIds) {
        List<LimitGroupConfig> res = new ArrayList<>(groupIds.size());
        Set<Map.Entry<String, LimitGroupConfig>> entries = BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.entrySet();
        for (Map.Entry<String, LimitGroupConfig> entry : entries) {
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
                BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.put(groupId, objectMapper.readValue(configStr, LimitGroupConfig.class));
            }
        } catch (JsonProcessingException e) {
            throw new RedisAuxException("json序列化异常:" + e.getMessage());
        }
    }

    public void clear(String groupId) {
        BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.remove(groupId);
        redisTemplate.execute(delGroupScript, Collections.singletonList(CommonUtil.getLimiterConfigName(groupId) + "*"));
    }

    public List<LimitGroupConfig> getAll() {
        List<LimitGroupConfig> list = new LinkedList<>();
        if (BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.size() == 0) {
            List<String> res = (List<String>) redisTemplate.execute(getGroupScript, null);

            List<LimitGroupConfig> limitGroups = new LinkedList<>();
            if (res != null) {
                for (String s : res) {
                    try {
                        limitGroups.add(objectMapper.readValue(s, LimitGroupConfig.class));
                    } catch (JsonProcessingException e) {
                        throw new RedisAuxException("json序列化异常:" + e.getMessage());
                    }
                }
            }
        } else {
            for (LimitGroupConfig value : BaseRateLimiter.RATE_LIMIT_GROUP_CONFIG_MAP.values()) {
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


    public int handle(LimitGroupConfig limitGroupConfig, String ip,
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
