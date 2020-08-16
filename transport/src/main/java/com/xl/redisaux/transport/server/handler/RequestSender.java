package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.transport.server.http.HttpClientInitializer;
import com.xl.redisaux.transport.server.http.HttpClientMsgHandler;
import com.xl.redisaux.transport.vo.BaseVO;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import java.net.URI;


/**
 * @author lulu
 * @Date 2020/8/14 16:00
 * 通过netty client发送http请求
 */
public class RequestSender {
    public static String sendRequest(FullHttpRequest request, BaseVO vo) throws Exception {
        HttpClientMsgHandler h = new HttpClientMsgHandler();
        String host = vo.ip;
        int port = vo.port;
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).handler(new HttpClientInitializer(h));
        Channel ch = b.connect(host, port).sync().channel();
        ch.writeAndFlush(request);
        ch.closeFuture().sync();
        group.shutdownGracefully();
        return h.getResult();
    }

}
