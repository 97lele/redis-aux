package com.xl.redisaux.transport.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author lulu
 * @Date 2020/7/18 8:28
 */
public final class TransportConfig {

    public static final int DEFAULT_RECEIVE_HEART_PORT = 1210;


    public static final String HEARTBEAT_CLIENT_IP = "redisaux.client.ip";
    //控制台地址host
    public static final String CONSOLE_HOST = "redisaux.console.host";

    public static final String CONSOLE_PORT="redisaux.console.port";


    //客户端发送心跳包周期
    public static final String HEARTBEAT_INTERVAL_MS = "redisaux.heartbeat.interval.ms";
    //客户端服务端口
    public static final String HEARTBEAT_CLIENT_PORT = "server.port";

    public static final String HOST_NAME = "hostname";

    public static final String CONNECT_TIMEOUT_MS = "redisaux.heartbeat.connect.timeout";

    public static final String APPLICATION_NAME = "spring.application.name";

    private static final Map<String, String> paramMap = new ConcurrentHashMap<>();

    public static<T> T get(String key, Function<String,T> function){
        String s = paramMap.get(key);
        return s==null?null:function.apply(s);
    }
    public static String get(String key){
        return paramMap.get(key);
    }
    public static  void set(String key,String value){
        paramMap.put(key, value);
    }
}
