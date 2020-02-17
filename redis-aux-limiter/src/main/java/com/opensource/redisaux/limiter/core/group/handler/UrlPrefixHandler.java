package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;


/**
 * @author lulu
 * @Date 2020/2/17 10:38
 */
public class UrlPrefixHandler implements GroupHandler {
    private  int order = 1;

    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        String enablePrefix = limitGroupConfig.getEnableURLPrefix();
        if (url.startsWith(limitGroupConfig.getUnableURLPrefix())) {
            return LimiterConstants.WRONGPREFIX;
        }
        if ("/*".equals(enablePrefix) || url.startsWith(limitGroupConfig.getEnableURLPrefix())) {
            return LimiterConstants.PASS;
        }
        return LimiterConstants.CONTINUE;
    }

    public GroupHandler order(int order){
        this.order=order;
        return this;
    }


    @Override
    public int getOrder() {
        return order;
    }
}
