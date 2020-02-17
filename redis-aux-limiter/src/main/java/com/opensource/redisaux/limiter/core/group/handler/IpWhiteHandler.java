package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;

/**
 * @author lulu
 * @Date 2020/2/17 10:13
 */
public class IpWhiteHandler implements GroupHandler {
    private int order = 2;

    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        if (limitGroupConfig.isEnableWhiteList() &&
                IpCheckUtil.isFit(ip, limitGroupConfig.getWhiteRule())) {
            return LimiterConstants.PASS;
        }
        return LimiterConstants.CONTINUE;
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
