package com.opensource.redisaux.limiter.autoconfigure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.redisaux.common.CommonUtil;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.common.RedisAuxException;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import com.opensource.redisaux.limiter.core.group.handler.GroupHandlerList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/15 21:15
 */
@Component
public class LimiterGroupService {


    @Autowired
    @Qualifier(LimiterConstants.LIMITER)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private GroupHandlerList groupHandlers;

    private DefaultRedisScript getGroupScript;

    private DefaultRedisScript delGroupScript;

    private DefaultRedisScript updateCountScript;

    private DefaultRedisScript getCountScript;

    public LimiterGroupService() {
        groupHandlers = new GroupHandlerList();
        getGroupScript = new DefaultRedisScript();
        getGroupScript.setResultType(List.class);
        getGroupScript.setScriptText(getAllGroupScript());
        delGroupScript = new DefaultRedisScript();
        delGroupScript.setScriptText(delGroupScript());
        updateCountScript = new DefaultRedisScript();
        updateCountScript.setScriptText(updateCountStript());
        getCountScript = new DefaultRedisScript();
        getCountScript.setResultType(List.class);
        getCountScript.setScriptText(getCountScript());
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


    public void updateCount(boolean success, LimiteGroupConfig config) {
        long duringMills = config.getCountDuringUnit().toMillis(config.getCountDuring());
        String countName = CommonUtil.getLimiterCountName(config.getId(), success);
        List<String> param=Collections.singletonList(countName);
        long expire = 0L;
        long del = 0L;
        long current = System.currentTimeMillis();
//ARGV 1是代表统计数目，2为执行过期的标志，3为毫秒生存时间，4为是否删除的标志
        if (config.setStartTime(current, false)) {
            save(config, true, false);
            expire = 1L;
        } else {
            //判断统计时间是否过期
            if (current - config.getStartTime() > duringMills) {
                expire = 1L;
                del = 1L;
                config.setStartTime(current, true);
                save(config, true, false);
                String counNameOther=CommonUtil.getLimiterCountName(config.getId(),!success);
                param=Arrays.asList(countName,counNameOther);
            }
        }
        Long[] args = new Long[]{System.nanoTime(), expire, duringMills, del};
        redisTemplate.execute(updateCountScript, param, args);

    }

    public Map<String, String> getCount(String groupId) {
        String fail = CommonUtil.getLimiterCountName(groupId, false);
        String success = CommonUtil.getLimiterCountName(groupId, true);
        List execute = (List) redisTemplate.execute(getCountScript, Arrays.asList(fail, success));
        Long failCount = (Long) execute.get(0);
        Long successCount = (Long) execute.get(1);
        Long total = failCount + successCount;
        LimiteGroupConfig limiterConfig = getLimiterConfig(groupId);
        Map<String, String> map = new HashMap<>();
        map.put("fail", failCount.toString());
        map.put("success", successCount.toString());
        map.put("total", total.toString());
        long current = System.currentTimeMillis();
        String qps="0";
        if(limiterConfig.getStartTime()!=null&&total!=null){
            qps= CommonUtil.getQPS(total, TimeUnit.MILLISECONDS, current-limiterConfig.getStartTime());
        }
        map.put("qps", qps);

        return map;
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
        LimiteGroupConfig group = BaseRateLimiter.rateLimitGroupConfigMap.get(groupId);
        if (group == null) {
            reload(groupId);
        }
        return BaseRateLimiter.rateLimitGroupConfigMap.get(groupId);
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
                        e.printStackTrace();
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

    /**
     * ARGV 1是当前时间戳，2为执行过期的标志，3为毫秒生存时间，4为是否删除的标志
     *
     * @return
     */
    private String updateCountStript() {
        StringBuilder builder = new StringBuilder();
        builder.append("if tonumber(ARGV[4]) == 1 then redis.call('del',KEYS[1])\n redis.call('del',KEYS[2])\nend\n")
                .append("redis.call('pfadd',KEYS[1],ARGV[1])\n")
                .append("if tonumber(ARGV[2]) == 1 then redis.call('pexpire',KEYS[1],ARGV[3]) end");
        return builder.toString();
    }

    private String getCountScript() {
        StringBuilder builder = new StringBuilder();
        builder.append("local array={}\n")
                .append("array[1] = redis.call('pfcount',KEYS[1])\n")
                .append("array[2] = redis.call('pfcount',KEYS[2])\nreturn array");
        return builder.toString();
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
