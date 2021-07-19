package com.xl.redisaux.transport.client;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.transport.client.handler.DemoClientHandler;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;

import java.util.concurrent.ExecutionException;
//代表一个限流实例
public class InstanceInitlizerDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        InstanceRemoteService build = InstanceRemoteService.dashboard("127.0.0.1", 1210)
                .addHandler(new DemoClientHandler())
                .start();
        RemoteAction<String> request = RemoteAction.request(SupportAction.GET_RECORD_COUNT, "1");
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setPort(8082);
        instanceInfo.setIp("127.0.0.1");
        RemoteAction request1 = RemoteAction.request(SupportAction.SEND_SERVER_INFO, instanceInfo);
        build.performRequestOneWay(request1);
//        build.close();
        //发送消息
//        RemoteAction<ChangeLimitModeParam> request1 = RemoteAction.request(SupportAction.CHANGE_LIMIT_MODE, new ChangeLimitModeParam());
//        RemoteAction<TokenChangeParam> request2 = RemoteAction.request(SupportAction.TOKEN_CHANGE, new TokenChangeParam());
//        RemoteAction<ChangeIpRuleParam> request3 = RemoteAction.request(SupportAction.CHANGE_IP_RULE, new ChangeIpRuleParam());
//        RemoteAction<ChangeIpRuleParam> request = RemoteAction.request(SupportAction.GET_SERVER_INFO, new ChangeIpRuleParam());
//        ActionFuture actionFuture = build.performRequest(request1);
//        ActionFuture tokenFuture = build.performRequest(request2);
//        ActionFuture ipRuleAction = build.performRequest(request3);
//        ActionFuture actionFuture1 = build.performRequest(request);
//        RemoteAction remoteAction = actionFuture.get();
//        RemoteAction remoteAction1 = tokenFuture.get();
//        RemoteAction remoteAction2 = ipRuleAction.get();
//        System.out.println(remoteAction);
//        System.out.println(remoteAction1);
//        System.out.println(remoteAction2);
//        System.out.println(actionFuture1.get());
//        build.close();
    }
}
