package com.xl.redisaux.transport.server.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;

public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {
    HttpClientMsgHandler handler;

    public HttpClientInitializer(HttpClientMsgHandler httpClientMsgHandler) {
        this.handler = httpClientMsgHandler;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();
        p.addLast(new HttpClientCodec());
        //这里要添加解压，不然打印时会乱码
        p.addLast(new HttpContentDecompressor());
        //添加HttpObjectAggregator， HttpClientMsgHandler才会收到FullHttpResponse
        p.addLast(new HttpObjectAggregator(123433));
        p.addLast(handler);
    }
}