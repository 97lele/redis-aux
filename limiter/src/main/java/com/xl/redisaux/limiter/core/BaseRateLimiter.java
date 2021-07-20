package com.xl.redisaux.limiter.core;


import com.xl.redisaux.common.api.LimitGroupConfig;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:27
 */
@SuppressWarnings("unchecked")
public interface BaseRateLimiter {

    /**
     * 存放组的信息
     */
     Map<String, LimitGroupConfig> RATE_LIMIT_GROUP_CONFIG_MAP = new ConcurrentHashMap<>();



    /**
     * 配置修改
     * @param limitGroupConfigs
     */
     static void createOrUpdateGroups(List<LimitGroupConfig> limitGroupConfigs) {
        for (LimitGroupConfig group : limitGroupConfigs) {
            RATE_LIMIT_GROUP_CONFIG_MAP.put(group.getId(), group);
        }
    }

    /**
     * 单个修改
     * @param limitGroupConfig
     */
     static void createOrUpdateGroups(LimitGroupConfig limitGroupConfig) {
        RATE_LIMIT_GROUP_CONFIG_MAP.put(limitGroupConfig.getId(),limitGroupConfig);
    }


    /**
     * 限流情况下，是否可以通过执行
     *
     * @param redisLimiter
     * @param methodKey
     * @return
     */
     Boolean canExecute(Annotation redisLimiter, String methodKey);

     Boolean canExecute(LimitGroupConfig limitGroup, String methodKey);


}