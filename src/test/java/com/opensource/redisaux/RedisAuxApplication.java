package com.opensource.redisaux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author lulu
 * @Date 2020/1/11 17:43
 */
@SpringBootApplication
@EnableRedisAux(bloomFilterPath = "com.opensource.redisaux.entity")
public class RedisAuxApplication {
    public static void main(String[] args) {
        SpringApplication.run(RedisAuxApplication.class, args);
    }

}
