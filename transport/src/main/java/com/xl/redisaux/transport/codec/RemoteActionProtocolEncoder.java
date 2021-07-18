package com.xl.redisaux.transport.codec;

import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class RemoteActionProtocolEncoder extends MessageToMessageEncoder<RemoteAction> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction, List<Object> list) throws Exception {
        ByteBuf buffer = channelHandlerContext.alloc().buffer();
        remoteAction.encode(buffer);
        list.add(buffer);
    }
}
