package com.xl.redisaux.transport.vo;

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

    public NodeVO(String[] info) {
        this.ip = info[0];
        this.port = Integer.valueOf(info[1]);
        this.hostName = info[2];
        this.lastSendTime = Long.valueOf(info[3]);
        this.appName = info[4];
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
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

    public static String getName(String port,String ip){
        return port+"-"+ip;
    }

    @Override
    public String toString() {
        return "NodeVO{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                ", appName='" + appName + '\'' +
                ", lastSendTime=" + lastSendTime +
                ", hostName='" + hostName + '\'' +
                '}';
    }
}
