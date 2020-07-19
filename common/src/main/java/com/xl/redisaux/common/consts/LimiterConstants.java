package com.xl.redisaux.common.consts;

/**
 * @author lulu
 * @Date 2020/2/15 13:17
 */
public final class LimiterConstants {
    public final static String LIMITER = "limiter";
    public final static String ENABLE_GROUP="enableGroup";
    public final static String CONNECT_CONSOLE="connectConsole";
    public final static String SCAPATH = "com.xl.redisaux.limiter.autoconfigure";
    public final static int WINDOW_LIMITER = 1;
    public final static int TOKEN_LIMITER = 2;
    public final static int FUNNEL_LIMITER = 3;
    public final static String FUNNEL = "funnel";
    public final static String TOKEN = "token";
    public final static String WINDOW = "window";
    public final static String GROUP_LIMITER_ASPECT="GroupLimiterAspect";
    public final static String NORMAL_LIMITER_ASPECT="NormalLimiterAspect";
    public final static String CLIENTCONFIG="ClientConfig";
    public final static int WRONGPREFIX = -3;
    public final static int INBLACKLIST = -2;
    public final static int TOOMUCHREQUEST = -1;
    public final static int CONTINUE = 0;
    public final static int PASS = 1;

}
