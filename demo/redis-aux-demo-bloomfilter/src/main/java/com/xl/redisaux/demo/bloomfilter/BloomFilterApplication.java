package com.xl.redisaux.demo.bloomfilter;

import com.xl.redisaux.bloomfilter.annonations.EnableBloomFilter;
import com.xl.redisaux.demo.bloomfilter.service.BloomFilterTestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @Author tanjl11
 * @create 2020/7/19 20:59
 */
@SpringBootApplication
@EnableBloomFilter(bloomFilterPath = "com.xl.redisaux.demo.bloomfilter.entity")
public class BloomFilterApplication {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext run = SpringApplication.run(BloomFilterApplication.class, args);
        BloomFilterTestService bean = run.getBean(BloomFilterTestService.class);
        bean.lambdaTest();
        bean.simpleTest();
        bean.timeOutTest();
        run.stop();

    }
}
