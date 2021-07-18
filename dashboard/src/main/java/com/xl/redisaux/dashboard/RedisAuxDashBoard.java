package com.xl.redisaux.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.*;

/**
 * @author lulu
 * @Date 2020/7/19 13:35
 */
@SpringBootApplication
public class RedisAuxDashBoard {
    private static Logger log = LoggerFactory.getLogger(RedisAuxDashBoard.class);

    public static void main(String[] args) {
        SpringApplication.run(RedisAuxDashBoard.class, args);
//        Map<String, String> propertiesMap = getProperties();
//        String port = propertiesMap.get(TransportConfig.CONSOLE_PORT);
//        String prefix = propertiesMap.get(TransportConfig.URL_PREFIX);
//        String handlerPath = propertiesMap.get(TransportConfig.HANDLER_PATH);
//
//        RedisAuxNettyServer server = new RedisAuxNettyServer(HostNameUtil.getIp(), Integer.valueOf(port), prefix, handlerPath);
//        server.start();
    }

    public static Map<String,String> getProperties() {
        Properties properties;
        Map<String,String> map=new HashMap<>();
        try {
            properties = PropertiesLoaderUtils.loadAllProperties("dashboard.properties");
            //遍历取值
            Set<Map.Entry<Object, Object>> entry = properties.entrySet();
            for (Map.Entry<Object, Object> objectEntry : entry) {
                map.put(objectEntry.getKey().toString(),objectEntry.getValue().toString());
            }
            return map;
        } catch (IOException e) {
            log.error("无法读取配置dashboard.properties文件:"+e.getMessage());
        }
        return null;
    }
}
