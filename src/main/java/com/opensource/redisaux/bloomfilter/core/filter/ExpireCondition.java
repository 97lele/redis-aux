package com.opensource.redisaux.bloomfilter.core.filter;

import com.opensource.redisaux.RedisAuxException;

import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 21:26
 */
public final class ExpireCondition {
    protected String keyPrefix;
    protected String keyName;
    protected Long timeout;
    protected TimeUnit timeUnit;
    protected BaseCondition baseCondition;
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
     InnerInfo build(){
        if(Objects.isNull(keyName)){
            throw new RedisAuxException("key is null!");
        }
        this.timeUnit=Objects.isNull(timeUnit)?TimeUnit.SECONDS:timeUnit;
        this.timeout=Objects.isNull(timeout)?-1L:timeout;
        return new InnerInfo(this);
    }
    public static ExpireCondition create(){
        return new ExpireCondition();
    }

    public BaseCondition toBaseCondition(){
        if(this.baseCondition==null){
            this.baseCondition=BaseCondition.create().keyName(keyName).keyPrefix(keyPrefix);
        }
        return this.baseCondition;
    }

    @PreDestroy
    public void destory(){
        this.baseCondition=null;
    }
}
