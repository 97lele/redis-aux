package com.opensource.redisaux.bloomfilter.support.builder;

import com.opensource.redisaux.RedisAuxException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 21:26
 */
public class ExpireCondition {
    protected String keyPrefix;
    protected String keyName;
    protected Long timeout;
    protected TimeUnit timeUnit;
    public ExpireCondition keyPrefix(String keyPrefix){
        this.keyPrefix=keyPrefix;
        return this;
    }
    public ExpireCondition keyName(String keyName){
        this.keyName=keyName;
        return this;
    }
    public ExpireCondition timeout(Long timeuot){
        this.timeout=timeuot;
        return this;
    }
    public ExpireCondition timeUnit(TimeUnit timeUnit){
        this.timeUnit=timeUnit;
        return this;
    }
    public InnerInfo build(){
        if(Objects.isNull(keyName)){
            throw new RedisAuxException("key is null!");
        }
        this.timeUnit=Objects.isNull(timeUnit)?TimeUnit.SECONDS:timeUnit;
        this.timeout=Objects.isNull(timeout)?-1L:timeout;
        return new InnerInfo(this);
    }
    public static ExpireCondition of(){
        return new ExpireCondition();
    }
}
