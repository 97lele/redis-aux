package com.opensource.redisaux;

import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilter;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import com.opensource.redisaux.bloomfilter.support.builder.AddCondition;
import com.opensource.redisaux.bloomfilter.support.builder.BaseCondition;
import com.opensource.redisaux.entity.TestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


@SpringBootTest
class RedisAuxApplicationTests {

    @Autowired
    private RedisBloomFilter bloomFilter;
    @Resource(name = BloomFilterConsts.INNERTEMPLATE)
    private RedisTemplate redisTemplate;

    @Test
    void simpleTest() {
        String key = "testAdd";
        AddCondition addCondition = AddCondition.of().keyName(key);
        BaseCondition baseCondition = addCondition.asBaseCondition();
        bloomFilter.add(addCondition, "hello");
        System.out.println("contain he:"+bloomFilter.mightContain(baseCondition,"he"));
        System.out.println("contain hello:"+bloomFilter.mightContain(baseCondition,"hello"));
        //多值操作
        bloomFilter.addAll(addCondition,Arrays.asList("h","a","c"));
        System.out.println("before reset："+bloomFilter.mightContains(baseCondition,Arrays.asList("a","b","c")));
        //重置,注意如果开启了增长，对应新的布隆过滤器们也会重置
        bloomFilter.reset(baseCondition);

        System.out.println("after reset："+bloomFilter.mightContains(baseCondition,Arrays.asList("a","hello","qq")));
        System.out.println("before delete："+redisTemplate.hasKey(key));
        //删除,也会把增长相关的删除
        bloomFilter.remove(baseCondition);
        System.out.println("after delete："+redisTemplate.hasKey(key));

    }


    @Test
    void timeOutTest() {
        bloomFilter.add(AddCondition.of().keyName("a1").timeout(30L).timeUnit(TimeUnit.SECONDS), 1);
        bloomFilter.addAll(AddCondition.of().keyName("a4").timeUnit(TimeUnit.SECONDS).timeout(10L), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        bloomFilter.add(AddCondition.of().keyName("a2").timeout(11L).timeUnit(TimeUnit.SECONDS), 1);
        bloomFilter.add(AddCondition.of().keyName("a3").timeout(22L).timeUnit(TimeUnit.SECONDS), 1);
        System.out.println(bloomFilter.mightContain(BaseCondition.of().keyName("a1"), 1));
        try {
            TimeUnit.SECONDS.sleep(35L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(redisTemplate.keys("a*").size());
    }

    @Test
    public void lambdaTest() throws InterruptedException {
        bloomFilter.addAll(TestEntity::getName, Arrays.asList(13, 14, 15, 16));
        System.out.println(bloomFilter.mightContain(TestEntity::getName, 15));
        System.out.println(bloomFilter.mightContains(TestEntity::getName, Arrays.asList(13, 200)));
    }

    @Test
    public void growTest() {
        String key = "testGrow";
        String key2 = "testGrow2";
        AddCondition addCondition2 = AddCondition.of().keyName(key2).exceptionInsert(5L);
        AddCondition addCondition=AddCondition.of().keyName(key).exceptionInsert(5L).enableGrow(true);
        bloomFilter.addAll(addCondition2, Arrays.asList(1, 2, 3, 4));
        bloomFilter.addAll(addCondition2, Arrays.asList(5, 6, 7, 8));
        bloomFilter.addAll(addCondition2, Arrays.asList(9, 10, 11, 12));
        bloomFilter.addAll(addCondition2, Arrays.asList(13, 14, 15, 16));
        System.out.println("withOutGrow"+bloomFilter.mightContains(addCondition2.asBaseCondition(), Arrays.asList(13, 200)));
        bloomFilter.addAll(addCondition, Arrays.asList(1, 2, 3, 4));
        bloomFilter.addAll(addCondition, Arrays.asList(5, 6, 7, 8));
        bloomFilter.addAll(addCondition, Arrays.asList(9, 10, 11, 12));
        bloomFilter.addAll(addCondition, Arrays.asList(13, 14, 15, 16));
        System.out.println("withGrow:"+bloomFilter.mightContains(addCondition.asBaseCondition(), Arrays.asList(13, 200)));
    }


}
