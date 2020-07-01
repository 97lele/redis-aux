package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.IpRuleHolder;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author lulu
 * @Date 2020/2/17 10:13
 */
public class IpWhiteHandler implements GroupHandler {
    private int order = 2;
    private final IpRuleHolder holder;
    public IpWhiteHandler(){
        holder=new IpRuleHolder();
    }
    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        if (limitGroupConfig.isEnableBlackList()) {
            // 判断规则有无变
            String whiteRule = limitGroupConfig.getBlackRule();
            String id = limitGroupConfig.getId();
            if (!holder.getRule().equals(whiteRule)) {
                holder.setRule(whiteRule);
                holder.addRule(whiteRule, id);
            }
            if (IpCheckUtil.isFit(ip, holder.getRuleFromIp(ip, id))) {
                return LimiterConstants.PASS;
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
