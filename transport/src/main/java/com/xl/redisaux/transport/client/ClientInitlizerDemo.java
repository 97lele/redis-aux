package com.xl.redisaux.transport.client;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.transport.client.handler.DemoClientHandler;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ClientInitlizerDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ClientRemoteService build = ClientRemoteService.of("127.0.0.1",1210)
                .addHandler(new DemoClientHandler())
                .start();
        RemoteAction<String> request = RemoteAction.request(SupportAction.GET_RECORD_COUNT, "1");
        RemoteAction<Object> request1 = RemoteAction.request(SupportAction.GET_SERVER_INFO, null);
        ActionFuture actionFuture = build.performRequest(request);
        ActionFuture actionFuture1 = build.performRequest(request1);
        RemoteAction remoteAction = actionFuture.get();
        Map<String,Object> body = RemoteAction.getBody(Map.class, remoteAction);
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            System.out.println(entry.getKey()+":"+entry.getValue());
        }
        RemoteAction remoteAction1 = actionFuture1.get();
        ServerInfo serverInfo = RemoteAction.getBody(ServerInfo.class, remoteAction1);
        System.out.println(serverInfo);
        Set<String> groupIds = serverInfo.getGroupIds();
        RemoteAction<Set<String>> remoteAction2 = RemoteAction.request(SupportAction.GET_CONFIGS_BY_GROUPS, groupIds);
        ActionFuture actionFuture2 = build.performRequest(remoteAction2);
        RemoteAction response = actionFuture2.get();
        List<LimiteGroupConfig> res = RemoteAction.getBody(List.class, response);
        System.out.println(res);
        build.close();
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
