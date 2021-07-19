package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.server.handler.DashBoardInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class DashBoardRemoteService {
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
        if (channelInitializer == null) {
            channelInitializer = new DashBoardInitializer();
        }
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
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
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


    public static DashBoardRemoteService bind(int port) {
        return new DashBoardRemoteService().port(port);
    }

}
