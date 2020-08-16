package com.xl.redisaux.limiter.config;

import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.transport.config.TransportConfig;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import static com.xl.redisaux.transport.config.TransportConfig.*;

/**
 * @author lulu
 * @Date 2020/7/19 14:29
 */
public class ClientConfig implements InitializingBean {
    @Autowired
    Environment environment;
    private final static String DEFAULT_APPLICATION_NAME = "unknow";
    private final static Integer DEFAULT_LOST_MAX_COUNT = 3;
    private final static Long DEFAULT_LOST_MAX_MS = 3*1000L;


    @Override
    public void afterPropertiesSet() throws Exception {
        TransportConfig.set(HEARTBEAT_INTERVAL_MS, environment.getProperty(HEARTBEAT_INTERVAL_MS));
        TransportConfig.set(CONNECT_TIMEOUT_MS, environment.getProperty(CONNECT_TIMEOUT_MS));
        TransportConfig.set(HEARTBEAT_CLIENT_PORT, environment.getProperty(HEARTBEAT_CLIENT_PORT));
        String localIp = environment.getProperty(HEARTBEAT_CLIENT_IP);
        TransportConfig.set(HEARTBEAT_CLIENT_IP, localIp == null ? HostNameUtil.getIp() : localIp);
        String localHostName = environment.getProperty(HOST_NAME);
        TransportConfig.set(HOST_NAME, localHostName == null ? HostNameUtil.getHostName() : localHostName);
        TransportConfig.set(CONSOLE_IP, environment.getProperty(CONSOLE_IP));
        String consolePort = environment.getProperty(CONSOLE_PORT);
        TransportConfig.set(CONSOLE_PORT, consolePort == null ? DEFAULT_RECEIVE_HEART_PORT.toString() : consolePort);
        TransportConfig.set(APPLICATION_NAME, environment.getProperty(APPLICATION_NAME) == null ? DEFAULT_APPLICATION_NAME : environment.getProperty(APPLICATION_NAME));
        String maxMs = environment.getProperty(HEARTBEAT_LOST_MAX_MS);
        TransportConfig.set(HEARTBEAT_LOST_MAX_MS, maxMs ==null?DEFAULT_LOST_MAX_MS:maxMs);
        String maxCount = environment.getProperty(HEARTBEAT_LOST_MAX_COUNT);
        TransportConfig.set(HEARTBEAT_LOST_MAX_COUNT, maxCount==null?DEFAULT_LOST_MAX_COUNT:maxCount);
    }
}
