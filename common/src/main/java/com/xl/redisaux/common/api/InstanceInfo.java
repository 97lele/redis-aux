package com.xl.redisaux.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InstanceInfo {
    private String ip;
    private int port;
    private String hostName;
    private Set<String> groupIds;

    public String uniqueKey() {
        return ip + ":" + port;
    }
}
