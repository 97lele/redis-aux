package com.xl.redisaux.dashboard;

import com.xl.redisaux.common.utils.HostNameUtil;
import com.xl.redisaux.transport.config.TransportConfig;
import com.xl.redisaux.transport.server.HeartBeatServer;
import com.xl.redisaux.transport.server.SendRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author lulu
 * @Date 2020/7/19 13:35
 */
@SpringBootApplication
public class RedisAuxDashBoard {
    public static void main(String[] args) {
        String port = System.getProperty(TransportConfig.CONSOLE_PORT);
        HeartBeatServer server=new HeartBeatServer(HostNameUtil.getIp(), port==null?TransportConfig.DEFAULT_RECEIVE_HEART_PORT:Integer.valueOf(port));
        server.start();
        SpringApplication.run(RedisAuxDashBoard.class, args);

    }
    @Bean
    public SendRequest sendRequest(){
        return new SendRequest();
    }
}
