package com.xl.redisaux.limiter.component;


import com.xl.redisaux.transport.server.ServerRemoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

/**
 * 服务端
 */
public class DashBoardRequestHandler implements SmartLifecycle {
    @Autowired
    private Environment environment;

    private ServerRemoteService remoteService;

    @Override
    public void start() {
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
