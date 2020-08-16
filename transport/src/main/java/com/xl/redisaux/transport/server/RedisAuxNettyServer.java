package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.server.handler.DispatchHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author lulu
 * @Date 2020/7/18 20:43
 * netty心跳server
 */
public class RedisAuxNettyServer {
    private Logger log = LoggerFactory.getLogger(RedisAuxNettyServer.class);

    private final String host;
    private final int port;
    private static final ExecutorService SERVER =
            new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(1024), new DefaultThreadFactory("redis-aux-transport-server-scheduler", false), new ThreadPoolExecutor.CallerRunsPolicy());

    private DispatchHandler handler;

    public RedisAuxNettyServer(String host, int port, String prefix, String handlePath) {
        this.host = host;
        this.port = port;
        handler=DispatchHandler.getInstance();
        try {
            handler.init(prefix,handlePath);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

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
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(500*1024))
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new UserRequestNettyHandler(handler))
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8)).addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    .addLast(new IdleStateHandler(10, 0, 0))
                                    .addLast(new HeartBeatServerHandler())
                                    .addLast(new LineBasedFrameDecoder(1024))

                            ;
                        }
                    });

            try {
                ChannelFuture future = b.bind(host, port).sync();
                log.info("已就绪，等待客户端心跳,host:{}, port:{}", host, port);
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
