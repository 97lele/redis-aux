package com.xl.redisaux.transport.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lulu
 * @Date 2020/7/18 20:43
 * netty心跳server
 */
public class HeartBeatServer {

    private final String host;
    private final int port;
    private static final ExecutorService SERVER = Executors.newFixedThreadPool(1,
            new DefaultThreadFactory("redis-aux-transport-server-scheduler", true));

    public HeartBeatServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        SERVER.submit(() -> {
            NioEventLoopGroup boss = new NioEventLoopGroup(1);
            NioEventLoopGroup worker = new NioEventLoopGroup();
            ServerBootstrap b = new ServerBootstrap();
            b.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    //存放已完成三次握手的请求的队列的最大长度
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //启用心跳保活
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            channel.pipeline()
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8)).addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    .addLast(new IdleStateHandler(0, 0, 30))
                                    .addLast(new HeartBeatServerHandler())
                            ;
                        }
                    });

            try {
                ChannelFuture future = b.bind(host, port).sync();
//                System.out.println("已就绪，等待客户端心跳,host:" + host + ",port:" + port);
                future.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        });

    }


}
