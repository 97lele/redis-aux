package com.xl.redisaux.limiter.core.handler;

import com.xl.redisaux.limiter.config.LimiteGroupConfig;
import com.xl.redisaux.limiter.core.BaseRateLimiter;

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
