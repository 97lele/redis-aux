package com.xl.redisaux.transport.server.handler;

import com.xl.redisaux.common.api.ServerInfo;
import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.common.utils.IpCheckUtil;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@ChannelHandler.Sharable
public class SimpleHandler extends SimpleChannelInboundHandler<RemoteAction> {

    protected  int port;

    public SimpleHandler(int port){
        this.port=port;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction) throws Exception {
        if(!remoteAction.isResponse()){
            log.info("收到请求");
            log.info("请求ID，{}",remoteAction.getRequestId());
            log.info("编码，{}",remoteAction.getActionCode());
            log.info("实体内容，{}",remoteAction.getClazz());
            remoteAction.setResponse(true);
            Class<?> actionClass = SupportAction.getActionClass(remoteAction);
            if(actionClass.equals(ServerInfo.class)){
                ServerInfo serverInfo = new ServerInfo();
                serverInfo.setIp(HostNameUtil.getIp());
                serverInfo.setPort(port);
                remoteAction.setBody(serverInfo);
            }else{
                remoteAction.setBody(actionClass.newInstance());
            }
            int millis = ThreadLocalRandom.current().nextInt(5);
            log.info("睡眠，{}s",millis);
            TimeUnit.SECONDS.sleep(millis);
            channelHandlerContext.writeAndFlush(remoteAction);
        }
    }
}
