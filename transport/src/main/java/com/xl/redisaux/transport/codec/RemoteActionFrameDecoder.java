package com.xl.redisaux.transport.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RemoteActionFrameDecoder extends LengthFieldBasedFrameDecoder {
    public RemoteActionFrameDecoder() {
        super(Integer.MAX_VALUE, 0, 4, 0, 4);
    }
}
