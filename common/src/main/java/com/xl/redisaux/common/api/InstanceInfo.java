package com.xl.redisaux.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
public class InstanceInfo {
    private String ip;
    private int port;
    private String hostName;
    private Set<String> groupIds;
    private long connectedStartTime;

    public InstanceInfo(String ip, int port, String hostName) {
        this.hostName = hostName;
        this.ip = ip;
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstanceInfo that = (InstanceInfo) o;
        return port == that.port && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port);
    }

    public String uniqueKey() {
        return ip + ":" + port;
    }

    public static InstanceInfo uniqueKey2Instance(String uniqueKey) {
        String[] split = uniqueKey.split(":");
        if (split.length == 2) {
            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setIp(split[0]);
            instanceInfo.setPort(Integer.valueOf(split[1]));
            return instanceInfo;
        }
        return null;
    }

    public void setGroupIds(Set<String> groupIds) {
        this.groupIds = groupIds;
    }

    public void setConnectedStartTime(long connectedStartTime) {
        this.connectedStartTime = connectedStartTime;
    }
}
