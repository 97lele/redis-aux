package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;

/**
 * @author lulu
 * @Date 2020/2/17 10:20
 */
public class LimiteHandler implements GroupHandler {
    private int order=-1;
    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        return baseRateLimiter.canExecute(limitGroupConfig, methodKey)? LimiterConstants.PASS:LimiterConstants.TOOMUCHREQUEST;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public GroupHandler order(int order) {
        this.order=order;
        return this;
    }
}
