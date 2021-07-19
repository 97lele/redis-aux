package com.xl.redisaux.limiter.component;


import com.xl.redisaux.common.api.*;
import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.server.ServerRemoteService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 服务端
 */
@Slf4j
public class DashBoardRequestHandler implements SmartLifecycle {
    @Autowired
    private Environment environment;

    @Autowired
    private LimiterGroupService limiterGroupService;

    private ServerRemoteService remoteService;

    private volatile boolean isRunning;

    private Integer port;


    @Override
    public void start() {
        String port = environment.getProperty("redisaux.limiter.port", "1210");
        String timeOutSec = environment.getProperty("redisaux.limiter.keepAlive.timeout", "3");
        String retryCount = environment.getProperty("redisaux.limiter.keepAlive.lostMaxCount", "5");
        this.port = Integer.valueOf(port);
        remoteService = ServerRemoteService.of(this.port);
        remoteService.supportHeartBeat(Integer.valueOf(timeOutSec), Integer.valueOf(retryCount));
        remoteService.addHandler(new RequestActionHandler());
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

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, RemoteAction remoteAction) throws Exception {
            if (!remoteAction.isResponse()) {
                RemoteAction res = null;
                try {
                    res = doHandleAction(remoteAction);
                } catch (Exception e) {
                    log.error("error:", e);
                    res = RemoteAction.request(SupportAction.ERROR, e.getMessage());
                }
                channelHandlerContext.writeAndFlush(res);
            }
        }

        private RemoteAction doHandleAction(RemoteAction remoteAction) {
            SupportAction action = SupportAction.getAction(remoteAction);
            if (action.equals(SupportAction.GET_SERVER_INFO)) {
                ServerInfo serverInfo = new ServerInfo();
                serverInfo.setPort(port);
                serverInfo.setIp(HostNameUtil.getIp());
                serverInfo.setHostName(HostNameUtil.getHostName());
                serverInfo.setGroupIds(limiterGroupService.getGroupIds());
                return RemoteAction.response(action, serverInfo, remoteAction.getRequestId());
            }

            if (action.equals(SupportAction.GET_RECORD_COUNT)) {
                String body = RemoteAction.getBody(String.class, remoteAction);
                Map<String, Object> count = limiterGroupService.getCount(body);
                return RemoteAction.response(action, count, remoteAction.getRequestId());
            }
            if(action.equals(SupportAction.GET_CONFIG_BY_GROUP)){
                String body=RemoteAction.getBody(String.class,remoteAction);
                return RemoteAction.response(action,limiterGroupService.getLimiterConfig(body),remoteAction.getRequestId());
            }
            if(action.equals(SupportAction.GET_CONFIGS_BY_GROUPS)){
                Set<String> groupIds = RemoteAction.getBody(Set.class, remoteAction);
                return RemoteAction.response(action,limiterGroupService.getConfigByGroupIds(groupIds),remoteAction.getRequestId());
            }
            LimiteGroupConfig config = null;
            switch (action) {
                case FUNNEL_CHANGE:
                    FunnelChangeParam body = RemoteAction.getBody(FunnelChangeParam.class, remoteAction);
                    config = saveConfig(body, e -> e.setFunnelRateConfig(body.toConfig()));
                    break;
                case WINDOW_CHANGE:
                    WindowChangeParam windowChangeParam = RemoteAction.getBody(WindowChangeParam.class, remoteAction);
                    config = saveConfig(windowChangeParam, e -> e.setWindowRateConfig(windowChangeParam.toConfig()));
                    break;
                case TOKEN_CHANGE:
                    TokenChangeParam tokenChangeParam = RemoteAction.getBody(TokenChangeParam.class, remoteAction);
                    config = saveConfig(tokenChangeParam, e -> e.setTokenRateConfig(tokenChangeParam.toConfig()));
                    break;
                case CHANGE_IP_RULE:
                    ChangeIpRuleParam changeIpRuleParam = RemoteAction.getBody(ChangeIpRuleParam.class, remoteAction);
                    config = saveConfig(changeIpRuleParam, e -> {
                        if (changeIpRuleParam.getWhite()) {
                            e.setWhiteRule(changeIpRuleParam.getRule());
                            e.setEnableWhiteList(changeIpRuleParam.getEnable());
                        } else {
                            e.setBlackRule(changeIpRuleParam.getRule());
                            e.setEnableBlackList(changeIpRuleParam.getEnable());
                        }
                        return true;
                    });
                    break;
                case CHANGE_LIMIT_MODE:
                    ChangeLimitModeParam changeLimitModeParam = RemoteAction.getBody(ChangeLimitModeParam.class, remoteAction);
                    config = saveConfig(changeLimitModeParam, e -> {
                        Integer mode = changeLimitModeParam.getMode();
                        return mode < 4 && mode > 0 && e.setCurrentMode(mode);
                    });
                    break;
                case CHANGE_URL_RULE:
                    ChangeUrlRuleParam param = RemoteAction.getBody(ChangeUrlRuleParam.class, remoteAction);
                    config = saveConfig(param, e -> {
                        boolean change = false;
                        if (param.getEnableUrl() != null) {
                            change = true;
                            e.setEnableURLPrefix(param.getEnableUrl());
                        }
                        if (param.getUnableUrl() != null) {
                            change = true;
                            e.setUnableURLPrefix(param.getUnableUrl());
                        }
                        return change;
                    });
                    break;
                default:
                    return RemoteAction.request(SupportAction.ERROR, "code not found");
            }
            return RemoteAction.response(action, config, remoteAction.getRequestId());
        }

        private LimiteGroupConfig saveConfig(BaseParam param, Predicate<LimiteGroupConfig> setFunction) {
            LimiteGroupConfig limiterConfig = limiterGroupService.getLimiterConfig(param.getGroupId());
            boolean test = setFunction.test(limiterConfig);
            if (test) {
                limiterGroupService.save(limiterConfig, true, false);
            }
            return limiterConfig;
        }
    }
}
