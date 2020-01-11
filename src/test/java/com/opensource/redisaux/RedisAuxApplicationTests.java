package com.opensource.redisaux;

import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilter;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import com.opensource.redisaux.bloomfilter.support.builder.AddCondition;
import com.opensource.redisaux.bloomfilter.support.builder.BaseCondition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

@SpringBootTest
class RedisAuxApplicationTests {

    @Autowired
    private RedisBloomFilter bloomFilter;
    @Resource(name= BloomFilterConsts.INNERTEMPLATE)
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        bloomFilter.add(AddCondition.of().keyName("test"),1);
        System.out.println(bloomFilter.mightContain(BaseCondition.of().keyName("test"),1));
        bloomFilter.reset(BaseCondition.of().keyName("test"));
        System.out.println(bloomFilter.mightContain(BaseCondition.of().keyName("test"),1));
    }

}
