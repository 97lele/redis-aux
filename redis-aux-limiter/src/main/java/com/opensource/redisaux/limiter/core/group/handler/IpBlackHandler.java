package com.opensource.redisaux.limiter.core.group.handler;

import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.GroupHandler;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Supplier;

/**
 * @author lulu
 * @Date 2020/2/17 10:08
 */
public class IpBlackHandler implements GroupHandler {
    private int order = 3;
    @Override
    public int handle(LimiteGroupConfig limitGroupConfig, String ip, String url, BaseRateLimiter baseRateLimiter, String methodKey) {
        if (limitGroupConfig.isEnableBlackList()) {
            // 在黑名单中,不允许通过
            if (IpCheckUtil.isFit(ip, limitGroupConfig.getBlackRule())) {
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
        this.order=order;
        return this;
    }
}
