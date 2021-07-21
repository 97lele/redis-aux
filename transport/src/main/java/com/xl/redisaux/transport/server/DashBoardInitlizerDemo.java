package com.xl.redisaux.transport.server;


import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.server.handler.ConnectionHandler;
import com.xl.redisaux.transport.server.handler.SimpleHandler;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
//控制台
public class DashBoardInitlizerDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DashBoardRemoteService start = DashBoardRemoteService.bind(1210,null,null)
                .addHandler(new SimpleHandler())
                .supportHeartBeat(3, 3)
                .start();
        TimeUnit.SECONDS.sleep(5);
        Map<InstanceInfo, Channel> instanceMap = ConnectionHandler.getInstanceChannelMap();
        for (InstanceInfo s : instanceMap.keySet()) {
            ActionFuture actionFuture = DashBoardRemoteService.performRequest(RemoteAction.request(SupportAction.GET_RECORD_COUNT, "1"), s);
            RemoteAction remoteAction = actionFuture.get();
            Map<String,Object> body = RemoteAction.getBody(Map.class, remoteAction);
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                System.out.println("key:"+entry.getKey()+"value"+entry.getValue());
            }
        }

//        start.close();
    }
}
