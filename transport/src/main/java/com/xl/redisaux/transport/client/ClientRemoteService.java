package com.xl.redisaux.transport.client;

import com.xl.redisaux.transport.client.handler.ClientChannelInitializer;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ClientRemoteService {

    protected boolean heartBeat;

    protected int writeIdleSec;

    protected int port;

    protected String ip;

    protected Channel channel;

    protected Bootstrap bootstrap;

    protected ClientChannelInitializer channelInitializer;

    protected EventLoopGroup group;

    protected volatile boolean hasInit;

    public ClientRemoteService start() {
        if(!hasInit){
            init();
        }
        ChannelFuture sync = null;
        try {
            sync = bootstrap.connect(ip, port).sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("start fail:",e);
        }
        channel = sync.channel();
        return this;
    }

    public ClientRemoteService addHandler(ChannelInboundHandler...handlers){
        if(channelInitializer==null){
            channelInitializer=new ClientChannelInitializer();
        }
        channelInitializer.addSharableHandler(handlers);
        return this;
    }

    public synchronized ClientRemoteService init() {
        if(!hasInit){
            if(channelInitializer==null){
                throw new IllegalStateException("should only contain a handler");
            }
            if(heartBeat){
                channelInitializer.supportHeartBeat(writeIdleSec);
            }
            Bootstrap b = new Bootstrap();
            this.group = new NioEventLoopGroup();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(channelInitializer);
            this.bootstrap = b;
            hasInit=true;
        }
        return this;
    }

    public ClientRemoteService port(int port){
        this.port=port;
        return this;
    }
    public ClientRemoteService ip(String ip){
        this.ip=ip;
        return this;
    }

    public ClientRemoteService supportHeartBeat(int writeIdleSec){
        this.heartBeat=true;
        this.writeIdleSec=writeIdleSec;
        return this;
    }

    public ActionFuture performRequest(RemoteAction remoteAction) {
        channel.writeAndFlush(remoteAction);
        ActionFuture actionFuture = ResultHolder.putRequest(remoteAction);
        return actionFuture;
    }

    public void close(){
        channel.close().syncUninterruptibly();
        group.shutdownGracefully();
    }

    public static ClientRemoteService of(){
        return new ClientRemoteService();
    }
}
