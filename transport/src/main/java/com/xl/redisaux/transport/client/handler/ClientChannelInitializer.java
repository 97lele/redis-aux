package com.xl.redisaux.transport.client.handler;

import com.xl.redisaux.transport.client.InstanceRemoteService;
import com.xl.redisaux.transport.codec.BaseChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;
public class ClientChannelInitializer extends BaseChannelInitializer {


    public void supportHeartBeat(int writeIdleSec, int servletPort, InstanceRemoteService remoteService) {
        addNotSharableHandler(()->new IdleStateHandler(0, writeIdleSec, 0, TimeUnit.SECONDS), ()->new ClientHeartBeatHandler(servletPort,remoteService));
    }


}
