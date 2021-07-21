package com.xl.redisaux.transport.server.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServerHeartBeatHandler extends ChannelInboundHandlerAdapter {
    private int maxLost;
    private int curLost;

    public ServerHeartBeatHandler(int maxLost) {
        this.maxLost = maxLost;
    }

    //如果不活跃,除掉
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //发送心跳信息
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                if (++curLost >= maxLost) {
                    Channel channel = ctx.channel();
                    ConnectionHandler.unRegisterInstance(channel);
                    channel.close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public void resetLostTime(){
        curLost=0;
    }
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        Channel channel = ctx.channel();
        ConnectionHandler.unRegisterInstance(channel);
        channel.close();
    }
}
