package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.dashboard.config.DashboardConfig;
import com.xl.redisaux.transport.server.DashBoardRemoteService;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tanjl11
 * @date 2021/07/21 10:00
 */
@Component
public class InstanceManagerService implements SmartLifecycle {
    @Resource
    private Environment environment;

    private DashBoardRemoteService remoteService;

    @Resource
    private DashboardConfig config;

    private volatile boolean isRunning;


    private final Map<InstanceInfo, Set<String>> groupIdMap = new ConcurrentHashMap<>();


    @Override
    public void start() {
        remoteService = DashBoardRemoteService.bind(config.getPort())
                .supportHeartBeat(config.getMaxLost(), config.getIdleSec())
                .start();
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
        remoteService.close();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
