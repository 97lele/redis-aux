package com.xl.redisaux.transport.server;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import com.xl.redisaux.transport.server.handler.ConnectionHandler;
import com.xl.redisaux.transport.server.handler.DashBoardInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

@Slf4j
public class DashBoardRemoteService implements DisposableBean {
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected DashBoardInitializer channelInitializer;
    protected ServerBootstrap serverBootstrap;
    protected int port;
    protected Channel channel;
    protected volatile boolean hasInit;
    protected boolean supportHeartBeat;
    protected int maxLost;
    protected int readIdleSec;


    protected DashBoardRemoteService port(int port) {
        this.port = port;
        return this;
    }

    public DashBoardRemoteService supportHeartBeat(int maxLost, int readIdleSec) {
        this.readIdleSec = readIdleSec;
        this.supportHeartBeat = true;
        this.maxLost = maxLost;
        return this;
    }

    public DashBoardRemoteService addHandler(ChannelInboundHandler... handlers) {
        channelInitializer.addSharableHandler(handlers);
        return this;
    }

    public DashBoardRemoteService start() {
        if (!hasInit) {
            init();
        }
        ChannelFuture bind = null;
        try {
            bind = serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("server start fail:", e);
        }
        channel = bind.channel();
        return this;
    }

    public synchronized DashBoardRemoteService init() {
        if (!hasInit) {
            if (channelInitializer == null) {
                throw new IllegalStateException("should only contain a handler");
            }
            serverBootstrap = new ServerBootstrap();
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            if (supportHeartBeat) {
                channelInitializer.supportHeartBeat(readIdleSec, maxLost);
            }
            serverBootstrap.group(this.bossGroup, this.workerGroup)
                    .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_SNDBUF, 65536)
                    .childOption(ChannelOption.SO_RCVBUF, 65536)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(channelInitializer);
        }
        return this;
    }

    public void close() {
        channel.close().syncUninterruptibly();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        hasInit = false;
    }

    public static ActionFuture performRequest(RemoteAction<?> remoteAction, InstanceInfo instanceInfo) {
        return performRequest(remoteAction, instanceInfo, null);
    }

    public static ActionFuture performRequest(RemoteAction<?> remoteAction, InstanceInfo instanceInfo, Consumer<InstanceInfo> onInstanceMiss) {
        if (instanceInfo != null) {
            Channel channel = ConnectionHandler.getInstanceChannelMap().get(instanceInfo);
            if (channel == null) {
                if (onInstanceMiss != null) {
                    onInstanceMiss.accept(instanceInfo);
                }
                return null;
            }
            ActionFuture actionFuture = ResultHolder.putRequest(remoteAction);
            //向客户端发送请求
            channel.writeAndFlush(remoteAction);
            return actionFuture;
        }
        return null;
    }

    public static DashBoardRemoteService bind(int port) {
        DashBoardRemoteService dashBoardRemoteService = new DashBoardRemoteService();
        dashBoardRemoteService.channelInitializer = new DashBoardInitializer();
        return dashBoardRemoteService.port(port);
    }

    @Override
    public void destroy() throws Exception {
        close();
    }
}
