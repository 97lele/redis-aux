package com.xl.redisaux.transport.client;

/**
 * @author lulu
 * @Date 2020/7/18 8:12
 * 心跳发送，
 */
public interface HeartBeatSender {
    /**
     * 一定周期间隔（毫秒）发送心跳到控制台
     * @return
     * @throws Exception
     */
    void sendHeartbeat() throws Exception;


}
