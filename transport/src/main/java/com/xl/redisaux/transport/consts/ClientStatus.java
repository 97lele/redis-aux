package com.xl.redisaux.transport.consts;

/**
 * @author lulu
 * @Date 2020/7/18 18:06
 */
public class ClientStatus {
    public static final int CLIENT_FIRST_INIT=-1;
    public static final int CLIENT_STATUS_OFF = 0;
    public static final int CLIENT_STATUS_PENDING = 1;
    public static final int CLIENT_STATUS_STARTED = 2;
    private ClientStatus(){}
}
