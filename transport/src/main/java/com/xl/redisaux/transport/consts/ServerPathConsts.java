package com.xl.redisaux.transport.consts;

/**
 * @Author tanjl11
 * @create 2020/7/20 20:44
 */
public final class ServerPathConsts {
    private ServerPathConsts(){}
    public static final String PERFIX="/redis-aux";
    public static final String GETCOUNT="/getCount";
    public static final String TOKENCONFIG="/changeTokenConfig";
    public static final String WINDOWCONFIG="/changeWindowConfig";
    public static final String FUNNELCONFIG="/changeFunnelConfig";
    public static final String CHANGEMODE="/changeLimitMode";
    public static final String CHANGERULE="/changeUrlRule";
    public static final String CHANGEIPRULE="/changeIpRule";
    public static final String GETGROUPIDS="/getGroupIds";
    public static final String GETNODES="/getNodes";
}
