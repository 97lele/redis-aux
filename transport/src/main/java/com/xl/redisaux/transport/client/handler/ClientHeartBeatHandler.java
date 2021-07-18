package com.xl.redisaux.transport.client.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端如果x s内没有和服务端交互，发送心跳包
 */
@Slf4j
public class ClientHeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //发送心跳信息
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if(event.state()== IdleState.WRITER_IDLE){
                ctx.writeAndFlush("ping");
            }
        }else{
            super.userEventTriggered(ctx,evt);
        }
    }
}
