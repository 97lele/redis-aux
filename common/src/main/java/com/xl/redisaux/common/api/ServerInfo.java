package com.xl.redisaux.common.api;

import lombok.Data;

import java.util.Set;

@Data
public class ServerInfo {
    private String ip;
    private int port;
    private String hostName;
    private Set<String> groupIds;
}
