package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.transport.codec.BaseChannelInitializer;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class ServerChannelInitializer extends BaseChannelInitializer {

    public ServerChannelInitializer supportHeartBeat(int readIdleSec,int maxLost) {
        addNotSharableHandler(()->new IdleStateHandler(readIdleSec, 0, 0, TimeUnit.SECONDS), ()->new ServerHeartBeatHandler(maxLost));
        return this;
    }
}
