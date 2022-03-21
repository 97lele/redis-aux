package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 控制台定期接受客户端心跳
 * 控制台发送信息给客户端，客户端处理后，服务端做展示
 */
@Slf4j
@ChannelHandler.Sharable
public class SimpleHandler extends ChannelInboundHandlerAdapter {


    public SimpleHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RemoteAction remoteAction = (RemoteAction) msg;
        if (remoteAction.isResponse()) {
            log.trace("编码{}", remoteAction.getActionCode());
            log.trace("实体{}", remoteAction);
            log.trace("服务端收到结果{}", remoteAction.getBody());
            ResultHolder.onSuccess(remoteAction);
            ctx.writeAndFlush(remoteAction);
        }
    }

}
