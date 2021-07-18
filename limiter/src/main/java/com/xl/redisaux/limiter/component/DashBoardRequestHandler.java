package com.xl.redisaux.limiter.component;


import com.xl.redisaux.common.api.FunnelChangeParam;
import com.xl.redisaux.common.api.FunnelRateConfig;
import com.xl.redisaux.common.api.LimiteGroupConfig;
import com.xl.redisaux.common.enums.TimeUnitEnum;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.server.ServerRemoteService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

/**
 * 服务端
 */
public class DashBoardRequestHandler implements SmartLifecycle {
    @Autowired
    private Environment environment;

    @Autowired
    private LimiterGroupService limiterGroupService;

    private ServerRemoteService remoteService;

    private volatile boolean isRunning;

    @Override
    public void start() {
        String port = environment.getProperty("redisaux.limiter.port", "1210");
        String timeOutSec = environment.getProperty("redisaux.limiter.keepAlive.timeout", "3");
        String retryCount = environment.getProperty("redisaux.limiter.keepAlive.lostMaxCount", "5");
        remoteService = ServerRemoteService.of(Integer.valueOf(port));
        remoteService.supportHeartBeat(Integer.valueOf(timeOutSec), Integer.valueOf(retryCount));
        remoteService.addHandler(new RequestActionHandler(limiterGroupService));
        remoteService.start();
        isRunning = true;
    }

    @Override
    public void stop() {
        remoteService.close();
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @ChannelHandler.Sharable
    public class RequestActionHandler extends SimpleChannelInboundHandler<RemoteAction> {
        private LimiterGroupService groupService;

        public RequestActionHandler(LimiterGroupService limiterGroupService) {
            this.groupService = limiterGroupService;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction) throws Exception {
            if (!remoteAction.isResponse()) {
                Class<?> sourceClass = SupportAction.getActionClass(remoteAction);
                if(sourceClass.equals(FunnelChangeParam.class)){
                    FunnelChangeParam body = RemoteAction.getBody(FunnelChangeParam.class, remoteAction);
                    FunnelRateConfig funnelRateConfig = body.toConfig();
                    LimiteGroupConfig config = limiterGroupService.getLimiterConfig(body.getGroupId());
                    if(config.setFunnelRateConfig(funnelRateConfig)){
                        limiterGroupService.save(config, true, false);
                    }
                    RemoteAction<LimiteGroupConfig> response = RemoteAction.response(SupportAction.FUNNEL_CHANGE, config, remoteAction.getRequestId());
                    channelHandlerContext.writeAndFlush(response);
                }
            }
        }
    }
}
