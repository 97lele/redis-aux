package com.xl.redisaux.transport.client;

import com.xl.redisaux.transport.client.handler.ClientChannelInitializer;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InstanceRemoteService {

    protected boolean heartBeat;

    protected int writeIdleSec;

    protected int port;

    protected String ip;

    protected Channel channel;

    protected Bootstrap bootstrap;

    protected ClientChannelInitializer channelInitializer;

    protected EventLoopGroup group;

    protected volatile boolean hasInit;

    protected String channelId;

    protected int servletPort;

    public AtomicBoolean connected = new AtomicBoolean(false);

    private Runnable afterConnected;


    public synchronized InstanceRemoteService start() {
        if (!hasInit) {
            init();
        }
        doConnect();
        return this;
    }

    public boolean lostConnection() {
        return connected.compareAndSet(true, false);
    }

    public synchronized void doConnect() {
        ChannelFuture sync = null;
        while (!connected.get()) {
            try {
                sync = bootstrap.connect(ip, port).sync();
                connected.compareAndSet(false, true);
            } catch (Exception e) {
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException tmp) {

                }
                log.error("connect fail,ip:{},port:{}", ip, port, e);
            }
        }
        channel = sync.channel();
        channelId = channel.id().asShortText();
        if (afterConnected != null) {
            afterConnected.run();
        }
    }

    public InstanceRemoteService addHandler(ChannelInboundHandler... handlers) {
        if (channelInitializer == null) {
            channelInitializer = new ClientChannelInitializer();
        }
        channelInitializer.addSharableHandler(handlers);
        return this;
    }

    public synchronized InstanceRemoteService init() {
        if (!hasInit) {
            if (channelInitializer == null) {
                throw new IllegalStateException("should only contain a handler");
            }
            if (heartBeat) {
                channelInitializer.supportHeartBeat(writeIdleSec, servletPort, this);
            }
            Bootstrap b = new Bootstrap();
            this.group = new NioEventLoopGroup();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                    .option(ChannelOption.SO_SNDBUF, 65535)
                    .option(ChannelOption.SO_RCVBUF, 65535)
                    .handler(channelInitializer);
            this.bootstrap = b;
            hasInit = true;
        }
        return this;
    }

    protected InstanceRemoteService port(int port) {
        this.port = port;
        return this;
    }

    protected InstanceRemoteService ip(String ip) {
        this.ip = ip;
        return this;
    }

    public InstanceRemoteService servletPort(int port) {
        this.servletPort = port;
        return this;
    }

    public InstanceRemoteService afterConnected(Runnable afterConnected) {
        this.afterConnected = afterConnected;
        return this;
    }

    public InstanceRemoteService supportHeartBeat(int writeIdleSec) {
        this.heartBeat = true;
        this.writeIdleSec = writeIdleSec;
        return this;
    }

    public ActionFuture performRequest(RemoteAction remoteAction) {
        channel.writeAndFlush(remoteAction);
        ActionFuture actionFuture = ResultHolder.putRequest(remoteAction);
        return actionFuture;
    }

    public void performRequestOneWay(RemoteAction remoteAction) {
        channel.writeAndFlush(remoteAction);
    }

    public void close() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        hasInit = false;
    }

    public String getChannelId() {
        return channelId;
    }

    public static InstanceRemoteService dashboard(String ip, int port) {
        InstanceRemoteService service = new InstanceRemoteService()
                .ip(ip).port(port);
        return service
                ;
    }
}
