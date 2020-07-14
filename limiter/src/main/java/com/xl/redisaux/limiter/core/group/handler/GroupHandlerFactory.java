package com.xl.redisaux.limiter.core.group.handler;

import com.xl.redisaux.limiter.core.group.GroupHandler;

/**
 * @author lulu
 * @Date 2020/2/17 11:06
 */
public class GroupHandlerFactory {
    public static GroupHandler ipWhiteHandler() {
        return new IpWhiteHandler();
    }

    public static GroupHandler ipBlackHandler() {
        return new IpBlackHandler();
    }

    public static GroupHandler urlPrefixHandler() {
        return new UrlPrefixHandler();
    }

    public static GroupHandler limiteHandler() {
        return new LimiteHandler();
    }
}
