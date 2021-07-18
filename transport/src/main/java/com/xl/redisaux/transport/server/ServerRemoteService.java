package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.server.handler.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class ServerRemoteService {
    protected EventLoopGroup bossGroup;
    protected EventLoopGroup workerGroup;
    protected ServerChannelInitializer channelInitializer;
    protected ServerBootstrap serverBootstrap;
    protected int port;
    protected Channel channel;
    protected volatile boolean hasInit;
    protected boolean supportHeartBeat;
    protected int maxLost;
    protected int readIdleSec;



    public ServerRemoteService port(int port){
        this.port=port;
        return this;
    }

    public ServerRemoteService supportHeartBeat(int maxLost,int readIdleSec){
        this.readIdleSec=readIdleSec;
        this.supportHeartBeat=true;
        this.maxLost=maxLost;
        return this;
    }

    public ServerRemoteService addHandler(ChannelInboundHandler...handlers){
        if(channelInitializer==null){
            channelInitializer=new ServerChannelInitializer();
        }
        channelInitializer.addSharableHandler(handlers);
        return this;
    }

    public ServerRemoteService start(){
        if(!hasInit){
            init();
        }
        ChannelFuture bind = null;
        try {
            bind = serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("server start fail:",e);
        }
        channel=bind.channel();
        return this;
    }

    public synchronized ServerRemoteService init(){
        if(!hasInit){
            if(channelInitializer==null){
                throw new IllegalStateException("should only contain a handler");
            }
            serverBootstrap=new ServerBootstrap();
            bossGroup=new NioEventLoopGroup();
            workerGroup=new NioEventLoopGroup();
            if(supportHeartBeat){
                channelInitializer.supportHeartBeat(readIdleSec,maxLost);
            }
            serverBootstrap.group(bossGroup,workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(channelInitializer);
        }
        return this;
    }

    public void close(){
        channel.close().syncUninterruptibly();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
    public static ServerRemoteService of(){
        return new ServerRemoteService();
    }
}
