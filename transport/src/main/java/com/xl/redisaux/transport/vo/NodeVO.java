package com.xl.redisaux.transport.vo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author lulu
 * @Date 2020/7/18 20:32
 */
public class NodeVO {
    private String ip;
    private Integer port;
    private String appName;
    private Long lastSendTime;
    private String hostName;
    private final AtomicBoolean hasInit = new AtomicBoolean(false);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private final AtomicLong firstFailTime = new AtomicLong(0);
    private Integer lostMaxCount;
    private Long lostMaxMS;


    public NodeVO() {

    }



    public void resetFail() {
        failCount.set(0);
        firstFailTime.set(0);
    }

    public boolean canBroke() {
        return (System.currentTimeMillis() - this.firstFailTime.get()) > lostMaxMS;
    }
    public boolean readIdle(){
        return this.failCount.incrementAndGet()>this.lostMaxCount;
    }

    public synchronized void resloveAndSetAttribute(String infoStr) {
        String[] info = infoStr.split("-");
        this.lastSendTime = Long.valueOf(info[3]);
        if (!hasInit.get()) {
            this.ip = info[0];
            this.port = Integer.valueOf(info[1]);
            this.hostName = info[2];
            this.lastSendTime = Long.valueOf(info[3]);
            this.appName = info[4];
            this.lostMaxMS = Long.valueOf(info[5]);
            this.lostMaxCount = Integer.valueOf(info[6]);
            hasInit.set(true);
        }
    }

    public String getIp() {
        return ip;
    }

    public Integer getPort() {
        return port;
    }

    public String getAppName() {
        return appName;
    }

    public Long getLastSendTime() {
        return lastSendTime;
    }

    public String getHostName() {
        return hostName;
    }

    public AtomicBoolean getHasInit() {
        return hasInit;
    }

    public AtomicInteger getFailCount() {
        return failCount;
    }

    public AtomicLong getFirstFailTime() {
        return firstFailTime;
    }

    public Integer getLostMaxCount() {
        return lostMaxCount;
    }

    public Long getLostMaxMS() {
        return lostMaxMS;
    }

    @Override
    public String toString() {
        return "NodeVO{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", appName='" + appName + '\'' +
                ", hostName='" + hostName + '\'' +
                ", failCount=" + failCount +
                '}';
    }
}
