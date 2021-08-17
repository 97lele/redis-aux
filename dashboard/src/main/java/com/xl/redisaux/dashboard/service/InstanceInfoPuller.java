package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.dashboard.config.DashboardConfig;
import com.xl.redisaux.transport.common.RemoteAction;
import com.xl.redisaux.transport.common.SupportAction;
import com.xl.redisaux.transport.dispatcher.ActionFuture;
import com.xl.redisaux.transport.server.DashBoardRemoteService;
import com.xl.redisaux.transport.server.handler.ConnectionHandler;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author tanjl11
 * @date 2021/07/21 11:04
 * 定时拉取服务端数据
 */
@Component
@Slf4j
public class InstanceInfoPuller implements SmartLifecycle {

    private static DashboardConfig dashboardConfig;
    @Resource
    private ThreadPoolTaskScheduler taskScheduler;
    protected volatile boolean isRunning;
    /**
     * 存放每个实例的任务
     */
    private final Map<InstanceInfo, ScheduledFuture<?>> map = new ConcurrentHashMap<>();

    private final Map<InstanceInfo, List<LimitGroupConfig>> configMap = new ConcurrentHashMap<>();

    @Resource
    public void setDashboardConfig(DashboardConfig dashboardConfig) {
        InstanceInfoPuller.dashboardConfig = dashboardConfig;
    }

    @Override
    public void start() {
        DashBoardRemoteService.bind(dashboardConfig.getPort())
                .supportHeartBeat(dashboardConfig.getMaxLost(), dashboardConfig.getIdleSec())
                .addHandler(new ConnectionHandler(), new InstanceMessageHandler())
                .start();
        //开启扫描的扫描注册上来的任务
        CronTrigger trigger = new CronTrigger(dashboardConfig.getCronOfScanInstance());
        taskScheduler.schedule(() -> {
            Map<InstanceInfo, Channel> instanceChannelMap = ConnectionHandler.getInstanceChannelMap();
            for (Map.Entry<InstanceInfo, Channel> entry : instanceChannelMap.entrySet()) {
                //代表有新的实例加入，添加信息拉取任务
                if (map.get(entry.getKey()) == null) {
                    addTask(entry.getKey());
                    //代表旧的任务
                }
            }
            //查找不在instanceMap的实例，取消任务
            checkAndCancelTask();
        }, trigger);
        isRunning = true;
    }

    @Override
    public void stop() {
        for (Map.Entry<InstanceInfo, ScheduledFuture<?>> entry : map.entrySet()) {
            entry.getValue().cancel(true);
        }
        map.clear();
        taskScheduler.shutdown();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }


    public static <R> R performRequest(String uniqueKey, Class<R> res, SupportAction supportAction, Object param) {
        ActionFuture actionFuture = DashBoardRemoteService.performRequest(RemoteAction.request(supportAction, param), InstanceInfo.uniqueKey2Instance(uniqueKey));
        RemoteAction remoteAction = null;
        try {
            remoteAction = actionFuture.get(dashboardConfig.getIdleSec(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error(String.format("请求失败,address:%s", uniqueKey), e);
        }
        return RemoteAction.getBody(res, remoteAction);
    }

    public void addTask(InstanceInfo instanceInfo) {
        if (instanceInfo == null) {
            return;
        }
        CronTrigger trigger = new CronTrigger(dashboardConfig.getCronOfInfoPuller());
        ScheduledFuture<?> schedule = taskScheduler.schedule(() -> {
            RemoteAction<?> remoteAction = null;
            try {
                ActionFuture actionFuture = DashBoardRemoteService.performRequest(RemoteAction.request(SupportAction.GET_GROUPS, null), instanceInfo, this::cancelTask);
                if (actionFuture == null) {
                    return;
                }
                remoteAction = actionFuture.get(dashboardConfig.getIdleSec(), TimeUnit.SECONDS);
                Set<String> groupIds = RemoteAction.getBody(Set.class, remoteAction);
                ActionFuture getConfigs = DashBoardRemoteService.performRequest(RemoteAction.request(SupportAction.GET_CONFIGS_BY_GROUPS, groupIds), instanceInfo, this::cancelTask);
                if (getConfigs == null) {
                    return;
                }
                remoteAction = getConfigs.get(dashboardConfig.getIdleSec(), TimeUnit.SECONDS);
                List<LimitGroupConfig> configs = RemoteAction.getBody(List.class, remoteAction);
                configMap.put(instanceInfo, configs);
            } catch (Exception e) {
                log.error(String.format("拉取信息失败,ip:%s,port:%s", instanceInfo.getIp(), instanceInfo.getPort()), e);
                cancelTask(instanceInfo);
            }
        }, trigger);
        map.put(instanceInfo, schedule);
    }

    public void cancelTask(InstanceInfo instanceInfo) {
        if (instanceInfo != null) {
            ScheduledFuture<?> scheduledFuture = map.get(instanceInfo);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
                map.remove(instanceInfo);
            }
        }

    }

    public void checkAndCancelTask() {
        Iterator<Map.Entry<InstanceInfo, ScheduledFuture<?>>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<InstanceInfo, ScheduledFuture<?>> cur = iterator.next();
            if (!ConnectionHandler.getInstanceChannelMap().containsKey(cur.getKey())) {
                cur.getValue().cancel(true);
                iterator.remove();
            }
        }
    }

    public List<LimitGroupConfig> getByInstanceInfo(InstanceInfo instanceInfo) {
        return configMap.get(instanceInfo);
    }

    public Map<InstanceInfo, List<LimitGroupConfig>> getConfigMap() {
        return configMap;
    }
}
