package com.xl.redisaux.transport.server.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;

import java.nio.charset.Charset;

public class HttpClientMsgHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private String result;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        String result = response.content().toString(Charset.forName("utf-8"));
        this.result = result;
    }

    public String getResult() {
        return result;
    }
}