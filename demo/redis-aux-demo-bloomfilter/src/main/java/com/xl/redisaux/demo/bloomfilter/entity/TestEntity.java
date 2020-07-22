package com.xl.redisaux.demo.bloomfilter.entity;

import com.xl.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.xl.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.xl.redisaux.bloomfilter.annonations.EnableBloomFilter;

/**
 * @Author tanjl11
 * @create 2020/7/19 21:00
 */
@BloomFilterPrefix
public class TestEntity {
    @BloomFilterProperty(exceptionInsert = 10,local = false)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
