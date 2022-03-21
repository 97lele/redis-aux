
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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;


/**
 * 服务端
 * 处理控制台发来的请求
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
            //这里就不使用future了
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

    /**
     * dashboard向客户端发起的请求
     */
    @ChannelHandler.Sharable
    public class RequestActionHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            RemoteAction remoteAction = (RemoteAction) msg;
            //如果是请求类型，返回
            if (!remoteAction.isResponse()) {
                RemoteAction res = null;
                try {
                    res = doHandleAction(remoteAction);
                } catch (Exception e) {
                    log.error("error:", e);
                    res = RemoteAction.response(SupportAction.ERROR, e.getMessage(), remoteAction.getRequestId());
                }
                ctx.writeAndFlush(res);
            } else {
                //响应类型，默认释放
                ReferenceCountUtil.release(msg);
            }
        }


        private RemoteAction doHandleAction(RemoteAction remoteAction) {
            SupportAction action = SupportAction.getAction(remoteAction);
            long start = System.currentTimeMillis();
            Object res = null;
            try {
                switch (action) {
                    case SEND_SERVER_INFO:
                        InstanceInfo instanceInfo = new InstanceInfo(HostNameUtil.getIp(), port, HostNameUtil.getHostName());
                        instanceInfo.setGroupIds(limiterGroupService.getGroupIds());
                        res = instanceInfo;
                        break;
                    case GET_GROUPS:
                        res = limiterGroupService.getGroupIds();
                        break;
                    case GET_RECORD_COUNT:
                        res = limiterGroupService.getCount(RemoteAction.getBody(String.class, remoteAction));
                        break;
                    case GET_CONFIG_BY_GROUP:
                        res = limiterGroupService.getLimiterConfig(RemoteAction.getBody(String.class, remoteAction));
                        break;
                    case GET_CONFIGS_BY_GROUPS:
                        res = limiterGroupService.getConfigByGroupIds(RemoteAction.getBody(Set.class, remoteAction));
                        break;
                    case FUNNEL_CHANGE:
                        FunnelChangeParam body = RemoteAction.getBody(FunnelChangeParam.class, remoteAction);
                        res = saveConfig(body, e -> e.setFunnelRateConfig(body.toConfig())).getFunnelRateConfig();
                        break;
                    case WINDOW_CHANGE:
                        WindowChangeParam windowChangeParam = RemoteAction.getBody(WindowChangeParam.class, remoteAction);
                        res = saveConfig(windowChangeParam, e -> e.setWindowRateConfig(windowChangeParam.toConfig())).getWindowRateConfig();
                        break;
                    case TOKEN_CHANGE:
                        TokenChangeParam tokenChangeParam = RemoteAction.getBody(TokenChangeParam.class, remoteAction);
                        res = saveConfig(tokenChangeParam, e -> e.setTokenRateConfig(tokenChangeParam.toConfig())).getTokenRateConfig();
                        break;
                    case CHANGE_IP_RULE:
                        ChangeIpRuleParam changeIpRuleParam = RemoteAction.getBody(ChangeIpRuleParam.class, remoteAction);
                        LimitGroupConfig limitGroupConfig = saveConfig(changeIpRuleParam, e -> {
                            if (changeIpRuleParam.getWhite()) {
                                e.setWhiteRule(changeIpRuleParam.getRule());
                                e.setEnableWhiteList(changeIpRuleParam.getEnable());
                            } else {
                                e.setBlackRule(changeIpRuleParam.getRule());
                                e.setEnableBlackList(changeIpRuleParam.getEnable());
                            }
                            return true;
                        });
                        res = changeIpRuleParam.getWhite() ? limitGroupConfig.getWhiteRule() : limitGroupConfig.getBlackRule();
                        break;
                    case CHANGE_LIMIT_MODE:
                        ChangeLimitModeParam changeLimitModeParam = RemoteAction.getBody(ChangeLimitModeParam.class, remoteAction);
                        res = saveConfig(changeLimitModeParam, e -> {
                            Integer mode = changeLimitModeParam.getMode();
                            return mode < 4 && mode > 0 && e.setCurrentMode(mode);
                        }).getCurrentMode();
                        break;
                    case CHANGE_URL_RULE:
                        ChangeUrlRuleParam param = RemoteAction.getBody(ChangeUrlRuleParam.class, remoteAction);
                        LimitGroupConfig config = saveConfig(param, e -> {
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
                        res = config.getEnableURLPrefix() + "@@" + config.getUnableURLPrefix();
                        break;
                    default:
                        return RemoteAction.response(SupportAction.ERROR, "code not found", remoteAction.getRequestId());
                }
                return RemoteAction.response(action, res, remoteAction.getRequestId());
            } catch (Exception e) {
                log.error("请求处理失败",e);
                return RemoteAction.response(SupportAction.ERROR, e.getMessage(), remoteAction.getRequestId());
            } finally {
                log.trace("请求处理耗时：{},编码:{},requestId:{}", System.currentTimeMillis() - start, remoteAction.getActionCode(), remoteAction.getRequestId());
            }

        }

        private LimitGroupConfig saveConfig(BaseParam param, Predicate<LimitGroupConfig> setFunction) {
            LimitGroupConfig limiterConfig = limiterGroupService.getLimiterConfig(param.getGroupId());
            boolean pass = setFunction.test(limiterConfig);
            if (pass) {
                limiterGroupService.save(limiterConfig, true, false);
            }
            return limiterConfig;
        }
    }
}

