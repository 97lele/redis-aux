package com.xl.redisaux.transport.server;


import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.server.handler.ConnectionHandler;
import com.xl.redisaux.transport.server.handler.SimpleHandler;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
//控制台
public class DashBoardInitlizerDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DashBoardRemoteService start = DashBoardRemoteService.bind(1210)
                .addHandler(new ConnectionHandler(),new SimpleHandler())
                .supportHeartBeat(30, 3)
                .start();
        TimeUnit.SECONDS.sleep(5);
        Map<String, Channel> instanceMap = ConnectionHandler.getInstanceMap();
        for (String s : instanceMap.keySet()) {
            ConnectionHandler.performRequest(RemoteAction.request(SupportAction.GET_RECORD_COUNT, "1"), s);
        }

//        start.close();
    }
}
