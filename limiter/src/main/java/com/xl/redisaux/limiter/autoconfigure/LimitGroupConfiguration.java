package com.xl.redisaux.limiter.autoconfigure;

import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.common.utils.HostNameUtil;
import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author tanjl11
 * @date 2021/07/20 16:31
 */
@Data
@ConfigurationProperties(prefix = "redisaux.limiter")
@Configuration
public class LimitGroupConfiguration {
    private boolean useConfig = true;
    List<LimitGroupConfig> groups;
    private DashboardConfig dashboard;


    @Setter
    public static class DashboardConfig {
        private String ip;
        private int port;
        //多少s内不活跃，断开连接
        private int idleSec;

        public int getIdleSec() {
            return idleSec == 0 ? 30 : idleSec;
        }

        public int getPort() {
            return port == 0 ? 1210 : port;
        }

        public String getIp() {
            return ip == null ? HostNameUtil.getIp() : ip;
        }
    }
}
