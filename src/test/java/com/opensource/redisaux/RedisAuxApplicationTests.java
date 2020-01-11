package com.opensource.redisaux;

import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilter;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.Arrays;

@SpringBootTest
class RedisAuxApplicationTests {

    @Autowired
    private RedisBloomFilter bloomFilter;
    @Resource(name= BloomFilterConsts.INNERTEMPLATE)
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        bloomFilter.add("","test",1);
        System.out.println(bloomFilter.mightContain("","test",1));
        bloomFilter.reset("","test");
        System.out.println(bloomFilter.mightContain("","test",1));
    }

}
