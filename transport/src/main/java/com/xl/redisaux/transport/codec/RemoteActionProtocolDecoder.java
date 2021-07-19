package com.xl.redisaux.transport.codec;

import com.xl.redisaux.transport.common.RemoteAction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

public class RemoteActionProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        //进来的是请求服务
        RemoteAction decode = RemoteAction.decode(byteBuf);
        list.add(decode);
    }

}
