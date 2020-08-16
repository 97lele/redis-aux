package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.vo.NodeVO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lulu
 * @Date 2020/7/18 20:10
 * 服务端负责接受心跳数据
 */
public class HeartBeatServerHandler extends ChannelInboundHandlerAdapter {
    public static Map<String, NodeVO> nodeVOMap = new ConcurrentHashMap<>();

    /**
     * 处理心跳包
     *
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String id = ctx.channel().id().asShortText();
        NodeVO nodeVO = nodeVOMap.get(id);
        nodeVO.resloveAndSetAttribute(msg.toString());
        nodeVOMap.put(id, nodeVO);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        String id = ctx.channel().id().asShortText();
        nodeVOMap.put(id, new NodeVO());
    }

    /**
     * 是对已经建立的连接来说的
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        String id = ctx.channel().id().asShortText();
        NodeVO nodeVO = nodeVOMap.get(id);
        if (nodeVO == null) {
            return;
        }
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent state = (IdleStateEvent) evt;
            //在一定时间内读空闲才会关闭链接
            if (state.state().equals(IdleState.READER_IDLE)) {
                //如果读空闲事件大于x次
                if (nodeVO.readIdle()) {
                    //第一次到现在的时间内超过n秒，并且超过x次失败，断开连接
                    if (nodeVO.canBroke()) {
                        ctx.channel().close();
                        nodeVOMap.remove(id);
                    } else {
                        nodeVO.resetFail();
                    }
                }

            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        nodeVOMap.remove(ctx.channel().id().asShortText());
        ctx.close();

        throw new RuntimeException("异常信息：\r\n" + cause.getMessage());
    }
}
