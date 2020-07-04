package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.utils.IpCheckUtil;
import com.opensource.redisaux.common.utils.IpRuleHolder;
import com.opensource.redisaux.common.consts.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;

/**
 * @author lulu
 * @Date 2020/2/17 10:08
 */
public class IpBlackHandler implements GroupHandler {
    private int order = 3;
    private final IpRuleHolder holder;
    public IpBlackHandler(){
         holder=new IpRuleHolder();
    }

    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        if (limitGroupConfig.isEnableBlackList()) {
            // 判断规则有无变
            String blackRule = limitGroupConfig.getBlackRule();
            String id = limitGroupConfig.getId();
            if (!holder.getRule().equals(blackRule)) {
                holder.setRule(blackRule);
                holder.addRule(blackRule, id);
            }
            if (IpCheckUtil.isFit(ip, holder.getRuleFromIp(ip, id))) {
                return LimiterConstants.INBLACKLIST;
            }
        }
        return LimiterConstants.CONTINUE;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public GroupHandler order(int order) {
        this.order = order;
        return this;
    }


}
