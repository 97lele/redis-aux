package com.xl.redisaux.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

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
    /**
     * 实例与配置的信息
     */
    private final Map<InstanceInfo, Map<String, LimitGroupConfig>> configMap = new HashMap<>();

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final WriteLock writeLock = readWriteLock.writeLock();
    private final ReadLock readLock = readWriteLock.readLock();
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            //定期拉取有的分组信息
            try {
                ActionFuture actionFuture = DashBoardRemoteService.performRequest(RemoteAction.request(SupportAction.GET_GROUPS, null), instanceInfo, this::cancelTask);
                if (actionFuture == null) {
                    return;
                }
                remoteAction = actionFuture.get(dashboardConfig.getIdleSec(), TimeUnit.SECONDS);
                Set<String> groupIds = RemoteAction.getBody(Set.class, remoteAction);
                //获取分组信息的详情
                if (groupIds == null || groupIds.isEmpty()) {
                    return;
                }
                ActionFuture getConfigs = DashBoardRemoteService.performRequest(RemoteAction.request(SupportAction.GET_CONFIGS_BY_GROUPS, groupIds), instanceInfo, this::cancelTask);
                if (getConfigs == null) {
                    return;
                }
                remoteAction = getConfigs.get(dashboardConfig.getIdleSec(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error(String.format("拉取信息失败,ip:%s,port:%s", instanceInfo.getIp(), instanceInfo.getPort()), e);
                cancelTask(instanceInfo);
            }
            //定期更新信息
            try {
                writeLock.lock();
                List data = RemoteAction.getBody(List.class, remoteAction);
                if (data != null && data.size() > 0) {
                    List<LimitGroupConfig> groupConfigs = objectMapper.convertValue(data, new TypeReference<List<LimitGroupConfig>>() {
                    });
                    writeResult(instanceInfo, groupConfigs);
                }

            } catch (Exception e) {
                log.error("实例信息写入本地失败", e);
            } finally {
                writeLock.unlock();
            }
        }, trigger);
        map.put(instanceInfo, schedule);
    }

    protected void writeResult(InstanceInfo instanceInfo, List<LimitGroupConfig> configs) {
        Map<String, LimitGroupConfig> groupConfigMap = configMap.get(instanceInfo);
        if (groupConfigMap == null) {
            groupConfigMap = new HashMap<>();
        }
        for (LimitGroupConfig config : configs) {
            groupConfigMap.put(config.getId(), config);
        }
        configMap.put(instanceInfo, groupConfigMap);
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

    public Collection<LimitGroupConfig> getByInstanceInfo(InstanceInfo instanceInfo) {
        return (Collection<LimitGroupConfig>) wrapByReadLock(() -> {
            Map<String, LimitGroupConfig> tempMap = configMap.get(instanceInfo);
            if (tempMap != null) {
                return tempMap.values();
            }
            return Collections.EMPTY_LIST;
        });
    }

    public Set<InstanceInfo> getInstanceInfo() {
        return (Set<InstanceInfo>) wrapByReadLock(() -> configMap.keySet());
    }

    public LimitGroupConfig getByParam(InstanceInfo instanceInfo, String id) {
        return (LimitGroupConfig) wrapByReadLock(() -> {
            Map<String, LimitGroupConfig> configMap = this.configMap.get(instanceInfo);
            if (configMap != null) {
                return configMap.get(id);
            }
            return null;
        });
    }

    public Object wrapByReadLock(Callable callable) {
        try {
            readLock.lock();
            return callable.call();
        } catch (Exception e) {
            log.error("获取失败", e);
        } finally {
            readLock.unlock();
        }
        return null;
    }
}
