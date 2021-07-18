package com.xl.redisaux.transport.client;

import com.xl.redisaux.common.api.ChangeIpRuleParam;
import com.xl.redisaux.common.api.ChangeLimitModeParam;
import com.xl.redisaux.common.api.TokenChangeParam;
import com.xl.redisaux.transport.client.handler.DemoClientHandler;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;

import java.util.concurrent.ExecutionException;

public class ClientInitlizerDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ClientRemoteService of = ClientRemoteService.of();
        ClientRemoteService build = of.ip("127.0.0.1")
                .port(8090)
                .addHandler(new DemoClientHandler())
                .init();
        build.start();
        //发送消息
        RemoteAction<ChangeLimitModeParam> request1 = RemoteAction.request(SupportAction.CHANGE_LIMIT_MODE, new ChangeLimitModeParam());
        RemoteAction<TokenChangeParam> request2 = RemoteAction.request(SupportAction.TOKEN_CHANGE, new TokenChangeParam());
        RemoteAction<ChangeIpRuleParam> request3 = RemoteAction.request(SupportAction.CHANGE_IP_RULE, new ChangeIpRuleParam());
        RemoteAction<ChangeIpRuleParam> request = RemoteAction.request(SupportAction.GET_IP_AND_PORT, new ChangeIpRuleParam());
        ActionFuture actionFuture = build.performRequest(request1);
        ActionFuture tokenFuture = build.performRequest(request2);
        ActionFuture ipRuleAction = build.performRequest(request3);
        ActionFuture actionFuture1 = build.performRequest(request);
        RemoteAction remoteAction = actionFuture.get();
        RemoteAction remoteAction1 = tokenFuture.get();
        RemoteAction remoteAction2 = ipRuleAction.get();
        System.out.println(remoteAction);
        System.out.println(remoteAction1);
        System.out.println(remoteAction2);
        System.out.println(actionFuture1.get());
//        build.close();
    }
}
