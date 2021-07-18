package com.xl.redisaux.common.api;

import lombok.Data;

@Data
public class ServerInfo {
    private String ip;
    private int port;
    private String hostName;
}
