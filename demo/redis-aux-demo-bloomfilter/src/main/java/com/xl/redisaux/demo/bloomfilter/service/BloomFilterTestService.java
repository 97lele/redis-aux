package com.xl.redisaux.demo.bloomfilter.service;

import com.xl.redisaux.bloomfilter.core.filter.AddCondition;
import com.xl.redisaux.bloomfilter.core.filter.BaseCondition;
import com.xl.redisaux.bloomfilter.core.filter.RedisBloomFilter;
import com.xl.redisaux.demo.bloomfilter.entity.TestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @Author tanjl11
 * @create 2020/7/19 21:06
 */
@Service
public class BloomFilterTestService {
    @Autowired
    private RedisBloomFilter bloomFilter;


    public void lambdaTest() throws InterruptedException {
        bloomFilter.addAll(TestEntity::getName, Arrays.asList(13, 14, 15, 16));
        System.out.println(bloomFilter.mightContain(TestEntity::getName, 15));
        System.out.println(bloomFilter.mightContains(TestEntity::getName, Arrays.asList(13, 200)));
    }

    public void timeOutTest(boolean isLocal) {
        bloomFilter.add(AddCondition.create().keyName("a1").timeout(30L).timeUnit(TimeUnit.SECONDS).local(isLocal), 1);
        bloomFilter.addAll(AddCondition.create().keyName("a4").timeUnit(TimeUnit.SECONDS).timeout(10L).local(isLocal), Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));
        bloomFilter.add(AddCondition.create().keyName("a2").timeout(11L).timeUnit(TimeUnit.SECONDS).local(isLocal), 1);
        bloomFilter.add(AddCondition.create().keyName("a3").timeout(22L).timeUnit(TimeUnit.SECONDS).local(isLocal), 1);
        System.out.println(bloomFilter.mightContain(BaseCondition.create().keyName("a1"), 1));
        try {
            TimeUnit.SECONDS.sleep(35L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(bloomFilter.containKey(BaseCondition.create().keyName("a1")));
    }


    public void simpleTest(boolean isLocal) {
        String key = "testAdd";
        //默认local为false
        AddCondition addCondition = AddCondition.create().keyName(key).local(isLocal);
        BaseCondition baseCondition = addCondition.toBaseCondition();
        bloomFilter.add(addCondition, "hello");
        System.out.println("contain he:"+bloomFilter.mightContain(baseCondition,"he"));
        System.out.println("contain hello:"+bloomFilter.mightContain(baseCondition,"hello"));
        //多值操作
        bloomFilter.addAll(addCondition, Arrays.asList("h","a","c"));
        System.out.println("before reset："+bloomFilter.mightContains(baseCondition,Arrays.asList("a","b","c")));
        //重置
        bloomFilter.reset(baseCondition);
        System.out.println("after reset："+bloomFilter.mightContains(baseCondition,Arrays.asList("a","hello","qq")));
        System.out.println("before delete："+bloomFilter.containKey(baseCondition));
        //删除
        bloomFilter.remove(baseCondition);
        System.out.println("after delete："+bloomFilter.containKey(baseCondition));
    }


}
