package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@ChannelHandler.Sharable
public class SimpleHandler extends SimpleChannelInboundHandler<RemoteAction> {


    public SimpleHandler() {
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction) throws Exception {
        if (remoteAction.isResponse()) {
            log.info("服务端收到结果{}", remoteAction.getBody());
            ResultHolder.onSuccess(remoteAction);
            channelHandlerContext.flush();
        }
    }

}
