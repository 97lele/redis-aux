package com.xl.redisaux.transport.codec;

import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class BaseChannelInitializer extends ChannelInitializer<NioSocketChannel> {
    private List<ChannelInboundHandler> sharableHandler;

    private List<Supplier<ChannelInboundHandler>> notSharableHandler;


    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline
                .addLast( new RemoteActionFrameDecoder()
                        , new RemoteActionFrameEncoder()
                        , new RemoteActionProtocolEncoder()
                        , new RemoteActionProtocolDecoder()
                        , new LoggingHandler(LogLevel.DEBUG)
                );
        if (sharableHandler != null) {
            for (ChannelInboundHandler inboundHandler : sharableHandler) {
                pipeline.addLast( inboundHandler);
            }
        }
        if (notSharableHandler != null) {
            for (Supplier<ChannelInboundHandler> supplier : notSharableHandler) {
                pipeline.addLast( supplier.get());
            }
        }
    }

    public void addSharableHandler(ChannelInboundHandler... handlers) {
        if (sharableHandler == null) {
            sharableHandler = new ArrayList<>(Math.max(handlers.length, 8));
        }
        for (ChannelInboundHandler handler : handlers) {
            if (!handler.getClass().isAnnotationPresent(Sharable.class)) {
                throw new IllegalStateException(String.format("%s should be a sharable handler", handler.getClass().getSimpleName()));
            }
            sharableHandler.add(handler);
        }
    }

    public void addNotSharableHandler(Supplier<ChannelInboundHandler>... suppliers) {
        if (notSharableHandler == null) {
            notSharableHandler = new ArrayList<>(Math.max(suppliers.length, 8));
        }
        notSharableHandler.addAll(Arrays.asList(suppliers));
    }

}
