package com.xl.redisaux.transport.client.handler;

import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class DemoClientHandler extends SimpleChannelInboundHandler<RemoteAction> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction) throws Exception {
        if(remoteAction.isResponse()){
            log.info("收到结果");
            log.info("请求ID，{}",remoteAction.getRequestId());
            log.info("编码，{}",remoteAction.getActionCode());
            log.info("实体内容，{}",remoteAction.getClazz());
            ResultHolder.onSuccess(remoteAction);
        }
    }
}
