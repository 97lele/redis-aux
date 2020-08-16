package com.xl.redisaux.transport.client;

import com.xl.redisaux.transport.consts.ClientStatus;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lulu
 * @Date 2020/7/18 18:03
 * 客户端记录心跳状态
 */
public class HeartBeatClientHandler extends ChannelInboundHandlerAdapter {
    Logger log=LoggerFactory.getLogger(HeartBeatClientHandler.class);

    private final AtomicInteger currentState;
    private final Runnable disconnectCallback;
    public HeartBeatClientHandler(AtomicInteger currentState, Runnable disconnectCallback) {
        this.currentState = currentState;
        this.disconnectCallback = disconnectCallback;
    }

    /**
     * 控制台无响应时记录
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        currentState.set(ClientStatus.CLIENT_STATUS_OFF);
        disconnectCallback.run();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        currentState.set(ClientStatus.CLIENT_STATUS_STARTED);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        log.error(cause.getMessage());
    }
}
