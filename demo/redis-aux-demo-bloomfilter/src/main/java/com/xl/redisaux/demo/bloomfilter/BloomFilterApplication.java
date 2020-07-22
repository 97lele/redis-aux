package com.xl.redisaux.demo.bloomfilter;

import com.xl.redisaux.bloomfilter.annonations.EnableBloomFilter;
import com.xl.redisaux.demo.bloomfilter.service.BloomFilterTestService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

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
        bean.simpleTest(false);
        bean.timeOutTest(true);
        run.stop();

    }
}
