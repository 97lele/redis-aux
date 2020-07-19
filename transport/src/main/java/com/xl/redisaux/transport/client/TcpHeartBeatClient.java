package com.xl.redisaux.transport.client;

import com.xl.redisaux.transport.config.TransportConfig;
import com.xl.redisaux.transport.consts.ClientStatus;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.StringUtil;

import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lulu
 * @Date 2020/7/18 19:30
 * 心跳包客户端
 */
public class TcpHeartBeatClient implements HeartBeatSender {

    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private final AtomicInteger failConnectedTime = new AtomicInteger(0);
    private final AtomicBoolean shouldRetry = new AtomicBoolean(true);
    private final AtomicInteger currentState;
    private final String host;
    private final int port;
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2,
            new DefaultThreadFactory("redis-aux-transport-client-scheduler", true));

    public TcpHeartBeatClient(String host, int port) {
        if (StringUtil.isNullOrEmpty(host) || port <= 0) {
            throw new RuntimeException("server info error");
        }
        this.host = host;
        this.port = port;
        this.currentState = new AtomicInteger(ClientStatus.CLIENT_FIRST_INIT);
    }

    /**
     * 初始化客户端bootstrap，准备远程进行连接
     *
     * @return
     */
    private Bootstrap initClientBootstrap()  {
        Bootstrap b = new Bootstrap();
        eventLoopGroup = new NioEventLoopGroup();
        b.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                //增大一次发送的量
                .option(ChannelOption.TCP_NODELAY, true)
                //对象池，重用缓冲区
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TransportConfig.get(TransportConfig.CONNECT_TIMEOUT_MS, Integer::valueOf))
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new StringEncoder()).addLast(new StringDecoder()).addLast(new HeartBeatClientHandler(currentState, retryTask));
                    }
                });
        return b;
    }

    /**
     * 发送心跳包
     *
     * @throws Exception
     */
    @Override
    public void sendHeartbeat() throws Exception {
        String data = genData();
        System.out.println(data);
        channel.writeAndFlush(data);
    }


    /**
     * 重新链接
     */
    protected Runnable retryTask = () -> {

        if (!shouldRetry.get()) {
            return;
        }

        SCHEDULER.schedule(() -> {
            if (shouldRetry.get()) {
                try {
                    start();
                } catch (Exception e) {
                    throw new RuntimeException("retry Connect Fail" + e.getMessage());
                }
            }
        }, (failConnectedTime.get() + 1), TimeUnit.SECONDS);

    };

    public void start() throws Exception {
        SCHEDULER.schedule(()->{
            if (currentState.get() == ClientStatus.CLIENT_FIRST_INIT) {
                currentState.compareAndSet(ClientStatus.CLIENT_FIRST_INIT, ClientStatus.CLIENT_STATUS_OFF);
            }
            shouldRetry.set(true);
            connect(initClientBootstrap());
        },0,TimeUnit.SECONDS);

    }

    private void cleanUp() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
    }

    public void stop() {
        shouldRetry.set(false);
        while (currentState.get() == ClientStatus.CLIENT_STATUS_PENDING) {
            try {
                Thread.sleep(200);
            } catch (Exception ex) {
                // Ignore.
            }
        }
        cleanUp();
        failConnectedTime.set(0);
    }

    //链接远程console
    private void connect(Bootstrap b) {

        if (currentState.compareAndSet(ClientStatus.CLIENT_STATUS_OFF, ClientStatus.CLIENT_STATUS_PENDING)) {
            try {
                ChannelFuture result = b.connect(host, port).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            failConnectedTime.incrementAndGet();
                            channel = null;
                        } else {
                            failConnectedTime.set(0);
                            channel = future.channel();
                            SCHEDULER.scheduleAtFixedRate(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (channel != null && channel.isActive()) {
                                            sendHeartbeat();
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, 0, TransportConfig.get(TransportConfig.HEARTBEAT_INTERVAL_MS, Long::valueOf), TimeUnit.MILLISECONDS);
                        }
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public AtomicInteger getCurrentState() {
        return currentState;
    }

    /**
     * 心跳包内容
     *
     * @return
     */
    private static String genData() {
        String ip = TransportConfig.get(TransportConfig.HEARTBEAT_CLIENT_IP);
        String port = TransportConfig.get(TransportConfig.HEARTBEAT_CLIENT_PORT);
        String hostName = TransportConfig.get(TransportConfig.HOST_NAME);
        String current = String.valueOf(System.currentTimeMillis());
        String appName = TransportConfig.get(TransportConfig.APPLICATION_NAME);
        StringJoiner joiner = new StringJoiner("-");
        joiner.add(ip).add(port).add(hostName).add(current).add(appName);
        return joiner.toString();
    }
}
