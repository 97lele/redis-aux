package com.opensource.redisaux.bloomfilter.support.builder;

import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilter;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 20:48
 */
public class AddCondition {
    protected Double fpp;
    protected Long exceptionInsert;
    protected String keyPrefix;
    protected String keyName;
    protected Long timeout;
    protected TimeUnit timeUnit;


    public AddCondition fpp(Double fpp){
        this.fpp=fpp;
        return this;
    }
    public AddCondition exceptionInsert(Long exceptionInsert){
        this.exceptionInsert=exceptionInsert;
        return this;
    }
    public AddCondition keyPrefix(String keyPrefix){
        this.keyPrefix=keyPrefix;
        return this;
    }
    public AddCondition keyName(String keyName){
        this.keyName=keyName;
        return this;
    }
    public AddCondition timeout(Long timeuot){
        this.timeout=timeuot;
        return this;
    }
    public AddCondition timeUnit(TimeUnit timeUnit){
        this.timeUnit=timeUnit;
        return this;
    }
    public InnerInfo build(){
        if(Objects.isNull(keyName)){
            throw new RedisAuxException("key is null!");
        }
        this.fpp=Objects.isNull(fpp)?0.03:fpp;
        this.exceptionInsert=Objects.isNull(exceptionInsert)?1000L:exceptionInsert;
        this.timeUnit=Objects.isNull(timeUnit)?TimeUnit.SECONDS:timeUnit;
        this.timeout=Objects.isNull(timeout)?-1L:timeout;
        return new InnerInfo(this);
    }

    public static AddCondition of(){
        return new AddCondition();
    }

}
