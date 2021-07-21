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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@ChannelHandler.Sharable
@Slf4j
public class ConnectionHandler extends SimpleChannelInboundHandler<RemoteAction> {
    /**
     * ip:port(instanceInfo) channel
     */
    private final static Map<InstanceInfo, Channel> INSTANCE_CHANNEL_MAP = new ConcurrentHashMap<>();
    /**
     * channelId instaceInfo
     */
    private final static Map<String, InstanceInfo> CHANNEL_INSTANCE_MAP = new ConcurrentHashMap<>();

    private final Consumer<Channel> afterRegister;
    public ConnectionHandler(Consumer<Channel> afterRegister){
        this.afterRegister=afterRegister;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RemoteAction msg) throws Exception {
        //只处理心跳请求和心跳包信息
        SupportAction action = SupportAction.getAction(msg);
        if (action.equals(SupportAction.SEND_SERVER_INFO) || action.equals(SupportAction.HEART_BEAT)) {
            InstanceInfo body = RemoteAction.getBody(InstanceInfo.class, msg);
            log.info("收到心跳包信息或首次注册信息,{}", body);
            Channel channel = ctx.channel();
            registerInstance(body, channel);
            if(afterRegister!=null){
                afterRegister.accept(channel);
            }
            ctx.pipeline().get(ServerHeartBeatHandler.class).resetLostTime();
            ctx.flush();
        } else {
            ctx.write(msg);
        }
    }

    protected static void registerInstance(InstanceInfo instanceInfo, Channel channel) {
        String key = instanceInfo.uniqueKey();
        String channelId = channel.id().asShortText();
        instanceInfo.setConnectedStartTime(System.currentTimeMillis());
        if (!INSTANCE_CHANNEL_MAP.containsKey(instanceInfo)) {
            log.info("注册实例，{}", key);
            INSTANCE_CHANNEL_MAP.put(instanceInfo, channel);
            CHANNEL_INSTANCE_MAP.put(channelId, instanceInfo);
        }
    }

    protected static void unRegisterInstance(Channel channel) {
        String key = channel.id().asShortText();
        InstanceInfo remove = CHANNEL_INSTANCE_MAP.remove(key);
        if (Objects.nonNull(remove)) {
            log.info("移除不活跃的实例，{}", remove.uniqueKey());
            INSTANCE_CHANNEL_MAP.remove(remove);
        }
    }

    public static Set<InstanceInfo> listAll(){
        return INSTANCE_CHANNEL_MAP.keySet();
    }

    public static Map<InstanceInfo, Channel> getInstanceChannelMap() {
        return INSTANCE_CHANNEL_MAP;
    }

    public static InstanceInfo getInstanceInfoByChannel(Channel channel){
        return CHANNEL_INSTANCE_MAP.get(channel.id().asShortText());
    }

}
