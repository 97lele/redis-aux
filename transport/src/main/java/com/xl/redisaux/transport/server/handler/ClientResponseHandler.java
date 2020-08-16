package com.xl.redisaux.transport.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;


/**
 *
 */
public class ClientResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private String result;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, FullHttpResponse fullHttpResponse) throws Exception {
        result = fullHttpResponse.content().toString(Charset.forName("utf-8"));
    }

    public String getResult() {
        return result;
    }
}
