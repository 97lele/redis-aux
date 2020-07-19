package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.vo.NodeVO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.SocketAddress;
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
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println(msg.toString());
        String[] split = msg.toString().split("-");
        String name = NodeVO.getName(split[0], split[1]);
        NodeVO vo = new NodeVO(split);
        nodeVOMap.put(name, vo);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
//        System.out.println("有人过来了:"+ctx.channel().remoteAddress().toString());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        SocketAddress address = ctx.channel().remoteAddress();
        String s = address.toString();
//        System.out.println(s);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent state = (IdleStateEvent) evt;
            //在一定时间内读写空闲才会关闭链接
            if (state.state().equals(IdleState.WRITER_IDLE)) {
                ctx.channel().close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        throw new RuntimeException("异常信息：\r\n" + cause.getMessage());
    }
}
