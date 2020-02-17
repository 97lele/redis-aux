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

import java.util.*;

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

    private GroupHandlerList groupHandlers;

    private DefaultRedisScript getGroupScript;

    private DefaultRedisScript delGroupScript;

    public LimiterGroupService() {
        groupHandlers = new GroupHandlerList();
        getGroupScript = new DefaultRedisScript();
        getGroupScript.setResultType(List.class);
        getGroupScript.setScriptText(getAllGroupScript());
        delGroupScript = new DefaultRedisScript();
        delGroupScript.setScriptText(delGroupScript());
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
            String test = redisTemplate.opsForValue().get(CommonUtil.getLimiterConfigName(groupId)).toString();
            try {
                if (test != null && test.length() > 0) {
                    group = objectMapper.readValue(test, LimiteGroupConfig.class);
                    BaseRateLimiter.rateLimitGroupConfigMap.put(groupId, group);
                }
            } catch (JsonProcessingException e) {
                throw new RedisAuxException("json序列化异常:" + e.getMessage());
            }
        }
        return BaseRateLimiter.rateLimitGroupConfigMap.get(groupId);
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
        if (!groupHandlers.isSort()) {
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
