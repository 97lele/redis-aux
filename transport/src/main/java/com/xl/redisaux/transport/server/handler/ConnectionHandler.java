package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.dispatcher.ResultHolder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@ChannelHandler.Sharable
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler<RemoteAction> {

    private static Map<String, Channel> INSTANCE_MAP = new ConcurrentHashMap<>();

    private static Map<String, String> CHANNEL_INSTANCE_MAP = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemoteAction msg) throws Exception {
        //只处理心跳请求和心跳包信息
        SupportAction action = SupportAction.getAction(msg);
        if (action.equals(SupportAction.SEND_SERVER_INFO) || action.equals(SupportAction.HEART_BEAT)) {
            InstanceInfo body = RemoteAction.getBody(InstanceInfo.class, msg);
            log.info("收到心跳包信息或首次注册信息,{}", body);
            registerInstance(body, ctx.channel());
            ctx.flush();
        }
    }


    public static ActionFuture performRequest(RemoteAction remoteAction, String uniqueKey) {
        Channel channel = INSTANCE_MAP.get(uniqueKey);
        if (channel == null) {
            return null;
        }
        channel.writeAndFlush(remoteAction);
        ActionFuture actionFuture = ResultHolder.putRequest(remoteAction);
        return actionFuture;
    }

    protected static void registerInstance(InstanceInfo instanceInfo, Channel channel) {
        String key = instanceInfo.uniqueKey();
        String channelId = channel.id().asShortText();
        if(!CHANNEL_INSTANCE_MAP.containsKey(channelId)){
            log.info("注册实例，{}", key);
            INSTANCE_MAP.put(key, channel);
            CHANNEL_INSTANCE_MAP.put(channelId, key);
        }
    }

    protected static void unRegisterInstance(Channel channel) {
        String key = channel.id().asShortText();
        String uniqueKey = CHANNEL_INSTANCE_MAP.remove(key);
        if (Objects.nonNull(uniqueKey)) {
            log.info("移除不活跃的实例，{}", uniqueKey);
            INSTANCE_MAP.remove(uniqueKey);
        }
    }


    public static Map<String, Channel> getInstanceMap() {
        return INSTANCE_MAP;
    }
}
