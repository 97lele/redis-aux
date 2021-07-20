package com.xl.redisaux.limiter.core.handler;

import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.limiter.core.BaseRateLimiter;

/**
 * @author lulu
 * @Date 2020/2/17 10:20
 */
public class LimiteHandler implements GroupHandler {
    private int order=-1;
    @Override
    public int handle(LimitGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
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
