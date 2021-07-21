package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import com.xl.redisaux.transport.server.handler.ConnectionHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class InstanceMessageHandler extends SimpleChannelInboundHandler<RemoteAction> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemoteAction msg) throws Exception {
        if (msg.isResponse()) {
            log.info("服务端收到结果{}", msg.getBody());
            ResultHolder.onSuccess(msg);
            ctx.flush();
        }
    }

}
