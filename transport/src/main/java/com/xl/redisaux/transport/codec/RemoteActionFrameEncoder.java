package com.xl.redisaux.transport.codec;

import io.netty.handler.codec.LengthFieldPrepender;

public class RemoteActionFrameEncoder extends LengthFieldPrepender {
    public RemoteActionFrameEncoder() {
        super(4);
    }
}
