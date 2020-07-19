package com.xl.redisaux.limiter.core.handler;


/**
 * @author lulu
 * @Date 2020/2/17 11:06
 */
public final class GroupHandlerFactory {
    private GroupHandlerFactory(){};
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
