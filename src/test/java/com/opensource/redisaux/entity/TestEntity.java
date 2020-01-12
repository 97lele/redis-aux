package com.opensource.redisaux.entity;

import com.opensource.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.opensource.redisaux.bloomfilter.annonations.BloomFilterProperty;

/**
 * @author lulu
 * @Date 2020/1/12 20:50
 */
@BloomFilterPrefix
public class TestEntity {
    @BloomFilterProperty(enableGrow = true,exceptionInsert = 5,timeout = 30)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
