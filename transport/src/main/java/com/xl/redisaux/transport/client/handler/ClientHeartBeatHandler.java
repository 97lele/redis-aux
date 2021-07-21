package com.xl.redisaux.transport.client.handler;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.transport.client.InstanceRemoteService;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 客户端如果x s内没有和服务端交互，发送心跳包
 * 实例向dashboard发送
 */
@Slf4j
public class ClientHeartBeatHandler extends ChannelInboundHandlerAdapter {
    private int port;
    private InstanceRemoteService remoteService;

    protected ClientHeartBeatHandler(int port, InstanceRemoteService remoteService) {
        this.port = port;
        this.remoteService = remoteService;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        //发送心跳信息
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                InstanceInfo instanceInfo = new InstanceInfo(HostNameUtil.getIp(), port, HostNameUtil.getHostName());
                ctx.writeAndFlush(RemoteAction.response(SupportAction.HEART_BEAT, instanceInfo, -1));
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
        if (remoteService.lostConnection()) {
            remoteService.doConnect();
        }
        ;
    }
}
