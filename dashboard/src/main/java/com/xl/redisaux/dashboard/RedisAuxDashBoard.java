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
    public static void main(String[] args) {
        SpringApplication.run(RedisAuxDashBoard.class, args);
    }

}
