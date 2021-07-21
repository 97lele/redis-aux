package com.xl.redisaux.dashboard.config;

import com.xl.redisaux.common.utils.HostNameUtil;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
}
