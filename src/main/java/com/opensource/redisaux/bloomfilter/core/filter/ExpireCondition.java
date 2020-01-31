package com.opensource.redisaux.bloomfilter.core.filter;

import com.opensource.redisaux.RedisAuxException;
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
        if(keyName==null){
            throw new RedisAuxException("key is null!");
        }
        this.timeUnit=timeUnit==null?TimeUnit.SECONDS:timeUnit;
        this.timeout=timeout==null?-1L:timeout;
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

    protected void clear(){
        this.baseCondition=null;
    }
}
