package com.opensource.redisaux.test;

import com.opensource.redisaux.bloomfilter.autoconfigure.EnableBloomFilter;
import com.opensource.redisaux.limiter.autoconfigure.EnableLimiter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBloomFilter(bloomFilterPath = "com.opensource.redisaux.test.entity")
@EnableLimiter
public class RedisAuxTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisAuxTestApplication.class, args);
    }

}
