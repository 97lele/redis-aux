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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@SpringBootTest
class RedisAuxApplicationTests {

    @Autowired
    private RedisBloomFilter bloomFilter;
    @Resource(name= BloomFilterConsts.INNERTEMPLATE)
    private RedisTemplate redisTemplate;

    @Test
    void contextLoads() {
        bloomFilter.add(AddCondition.of().keyName("test1").timeout(30L).timeUnit(TimeUnit.SECONDS),1);
        bloomFilter.addAll(AddCondition.of().keyName("test4").timeUnit(TimeUnit.SECONDS).timeout(10L), Arrays.asList(1,2,3,4,5,6,7,8,9));
        bloomFilter.add(AddCondition.of().keyName("test2").timeout(11L).timeUnit(TimeUnit.SECONDS),1);
        bloomFilter.add(AddCondition.of().keyName("test3").timeout(22L).timeUnit(TimeUnit.SECONDS),1);
        try {
            TimeUnit.SECONDS.sleep(35L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(bloomFilter.mightContain(BaseCondition.of().keyName("test1"),1));
        bloomFilter.reset(BaseCondition.of().keyName("test"));
    }

    @Test
    public void

    testGrow(){
//        bloomFilter.addAll(AddCondition.of().keyName("testGrow").exceptionInsert(5L),Arrays.asList(1,2,3,4));
        bloomFilter.addAll(AddCondition.of().keyName("testGrow").exceptionInsert(5L),Arrays.asList(1,2,3,4));
        bloomFilter.addAll(AddCondition.of().keyName("testGrow"),Arrays.asList(88,21,11));
        bloomFilter.add(AddCondition.of().keyName("testGrow"),18);

        List<Boolean> testGrow = bloomFilter.mightContains(BaseCondition.of().keyName("testGrow"), Arrays.asList(2, 4, 9,8,88,220));
        System.out.println(testGrow);
        bloomFilter.reset(BaseCondition.of().keyName("testGrow"));
    }

    public static void main(String[] args) {
        List<Boolean> l = Arrays.asList(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
        List<Boolean> l2 = Arrays.asList(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
        List<List<Boolean>> keyList=new ArrayList<>();
        keyList.add(l);
        keyList.add(l2);


    }


}
