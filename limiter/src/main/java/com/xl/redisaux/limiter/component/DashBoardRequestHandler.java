package com.xl.redisaux.limiter.component;


import com.xl.redisaux.common.api.*;
import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.common.utils.NamedThreadFactory;
import com.xl.redisaux.limiter.autoconfigure.LimitGroupConfiguration;
import com.xl.redisaux.transport.client.InstanceRemoteService;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

/**
 * 服务端
 */
@Slf4j
public class DashBoardRequestHandler implements SmartLifecycle {
    @Resource
    private Environment environment;

    @Resource
    private LimiterGroupService limiterGroupService;

    @Resource
    private LimitGroupConfiguration config;

    private InstanceRemoteService remoteService;

    private volatile boolean isRunning;

    private Integer port;

    private String ip;

    private Integer servletPort;
    private ExecutorService dashBoardThread = Executors.newSingleThreadExecutor(new NamedThreadFactory("dashboard-client", true));

    @Override
    public void start() {
        LimitGroupConfiguration.DashboardConfig config = this.config.getDashboard();
        this.port = config.getPort();
        this.ip = config.getIp();
        this.servletPort = environment.getProperty("server.port", Integer.class);
        //dashboard默认地址
        remoteService = InstanceRemoteService.dashboard(ip, this.port);
        //如果使用文件配置，直接使用文件配置，否则代码自定义
        if (this.config.isUseConfig()) {
            List<LimitGroupConfig> groups = this.config.getGroups();
            limiterGroupService.saveAll(groups);
        }
        Runnable afterConnected = () -> {
            InstanceInfo instanceInfo = getInstanceInfo();
            remoteService.performRequestOneWay(RemoteAction.request(SupportAction.SEND_SERVER_INFO, instanceInfo));
        };
        dashBoardThread.execute(() -> remoteService.supportHeartBeat(config.getIdleSec())
                .addHandler(new RequestActionHandler())
                .servletPort(servletPort)
                .afterConnected(afterConnected)
                .start());
        isRunning = true;
    }

    @Override
    public void stop() {
        remoteService.close();
        isRunning = false;
        dashBoardThread.shutdown();
    }

    public InstanceInfo getInstanceInfo() {
        InstanceInfo instanceInfo = new InstanceInfo();
        instanceInfo.setIp(HostNameUtil.getIp());
        instanceInfo.setPort(servletPort);
        instanceInfo.setHostName(HostNameUtil.getHostName());
        return instanceInfo;
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
            if (action.equals(SupportAction.SEND_SERVER_INFO)) {
                InstanceInfo instanceInfo = new InstanceInfo();
                instanceInfo.setPort(port);
                instanceInfo.setIp(HostNameUtil.getIp());
                instanceInfo.setHostName(HostNameUtil.getHostName());
                instanceInfo.setGroupIds(limiterGroupService.getGroupIds());
                return RemoteAction.response(action, instanceInfo, remoteAction.getRequestId());
            }
            if(action.equals(SupportAction.GET_GROUPS)){
                Set<String> groupIds = limiterGroupService.getGroupIds();
                return RemoteAction.response(action,groupIds,remoteAction.getRequestId());
            }
            if (action.equals(SupportAction.GET_RECORD_COUNT)) {
                String body = RemoteAction.getBody(String.class, remoteAction);
                Map<String, Object> count = limiterGroupService.getCount(body);
                return RemoteAction.response(action, count, remoteAction.getRequestId());
            }
            if (action.equals(SupportAction.GET_CONFIG_BY_GROUP)) {
                String body = RemoteAction.getBody(String.class, remoteAction);
                return RemoteAction.response(action, limiterGroupService.getLimiterConfig(body), remoteAction.getRequestId());
            }
            if (action.equals(SupportAction.GET_CONFIGS_BY_GROUPS)) {
                Set<String> groupIds = RemoteAction.getBody(Set.class, remoteAction);
                return RemoteAction.response(action, limiterGroupService.getConfigByGroupIds(groupIds), remoteAction.getRequestId());
            }
            LimitGroupConfig config = null;
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

        private LimitGroupConfig saveConfig(BaseParam param, Predicate<LimitGroupConfig> setFunction) {
            LimitGroupConfig limiterConfig = limiterGroupService.getLimiterConfig(param.getGroupId());
            boolean test = setFunction.test(limiterConfig);
            if (test) {
                limiterGroupService.save(limiterConfig, true, false);
            }
            return limiterConfig;
        }
    }
}
