package com.xl.redisaux.transport.server;

import com.xl.redisaux.transport.server.handler.DispatchHandler;
import com.xl.redisaux.transport.server.handler.UserRequestHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author tanjl11@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/8/14 13:43
 *  修改内容:
 * </pre>
 */
public class UserRequestNettyHandler extends ChannelInboundHandlerAdapter {
    private DispatchHandler dispatchHandler;

    public UserRequestNettyHandler(DispatchHandler dispatchHandler) {
        this.dispatchHandler = dispatchHandler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest req = (FullHttpRequest) msg;
            try {
                // 1.获取URI
                String uri = req.uri();
                // 2.获取请求体
                ByteBuf buf = req.content();
                String content = buf.toString(CharsetUtil.UTF_8);
                // 3.获取请求方法
                HttpMethod method = req.method();
                // 4.获取请求头
                HttpHeaders headers = req.headers();
                //先把要触发的任务丢进任务池里，netty再一个个发送
                UserRequestHandler dispatch = dispatchHandler.dispatch(uri);
                Object result = dispatch.handle(method, content, headers);
                response(ctx, result);
            } finally {
                req.release();
            }
        }
    }

    private void response(ChannelHandlerContext ctx, Object result) {
        // 1.设置响应
        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8));
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        // 2.发送
        // 注意必须在使用完之后，close channel
        ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
}

