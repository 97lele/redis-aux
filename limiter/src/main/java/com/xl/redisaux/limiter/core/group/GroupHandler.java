package com.xl.redisaux.limiter.core.group;


import com.xl.redisaux.limiter.core.BaseRateLimiter;
import com.xl.redisaux.limiter.core.group.config.LimiteGroupConfig;

/**
 * @author lulu
 * @Date 2020/2/17 10:05
 */

public interface GroupHandler {


    /**
     *
     * 具体看constats
     * @return
     */
    int handle(LimiteGroupConfig limitGroupConfig, String ip,
               String url, BaseRateLimiter baseRateLimiter,
               String methodKey
               );

    int getOrder();

    GroupHandler order(int order);
}
