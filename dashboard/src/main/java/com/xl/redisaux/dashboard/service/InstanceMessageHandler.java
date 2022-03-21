package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务端接受响应结果，并释放
 */
@Slf4j
@ChannelHandler.Sharable
public class InstanceMessageHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RemoteAction remoteAction = (RemoteAction) msg;
        //如果是结果，不再继续访问
        if (remoteAction.isResponse()) {
            log.trace("消息:{}", msg);
            log.trace("服务端收到结果:{}", remoteAction.getBody());
            ResultHolder.onSuccess(remoteAction);
            //引用计数法清除
            ReferenceCountUtil.release(msg);
        } else {
            ctx.fireChannelRead(ctx);
        }
    }


}
