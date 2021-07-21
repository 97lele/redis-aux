package com.xl.redisaux.dashboard.config;

import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.dashboard.service.InstanceMessageHandler;
import com.xl.redisaux.transport.server.DashBoardRemoteService;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author tanjl11
 * @date 2021/07/21 10:04
 */
@Configuration
@ConfigurationProperties(prefix = "redisaux.limiter.dashboard")
@Setter
public class DashboardConfig {
    private int port;
    private int maxLost;
    private int idleSec;
    private String cronOfInfoPuller;

    public int getIdleSec() {
        return idleSec == 0 ? 30 : idleSec;
    }

    public int getPort() {
        return port == 0 ? 1210 : port;
    }

    public int getMaxLost() {
        return maxLost == 0 ? 3 : maxLost;
    }

    public String getCronOfInfoPuller() {
        if (cronOfInfoPuller == null || cronOfInfoPuller.isEmpty()) {
            return "0/5 * * * * ?";
        }
        return cronOfInfoPuller;
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("pull-instance-info");
        threadPoolTaskScheduler.setPoolSize(Runtime.getRuntime().availableProcessors() + 1);
        threadPoolTaskScheduler.setDaemon(true);
        threadPoolTaskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(false);
        threadPoolTaskScheduler.setRemoveOnCancelPolicy(true);
        return threadPoolTaskScheduler;
    }
}
