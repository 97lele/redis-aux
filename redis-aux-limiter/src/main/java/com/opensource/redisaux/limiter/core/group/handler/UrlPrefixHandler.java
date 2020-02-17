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
        String unableURLPrefix = limitGroupConfig.getUnableURLPrefix();
        for (String s : unableURLPrefix.split(";")) {
            if (url.startsWith(s)) {
                return LimiterConstants.WRONGPREFIX;
            }
        }
        if("/*".equals(enablePrefix)){
            return LimiterConstants.CONTINUE;
        }
        for (String s : enablePrefix.split(";")) {
            if(url.startsWith(s)){
                return LimiterConstants.PASS;
            }
        }
        return LimiterConstants.CONTINUE;
    }

    @Override
    public GroupHandler order(int order){
        this.order=order;
        return this;
    }


    @Override
    public int getOrder() {
        return order;
    }
}
