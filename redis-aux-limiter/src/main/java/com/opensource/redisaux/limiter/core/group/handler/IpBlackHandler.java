package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.IpRuleHolder;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

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
