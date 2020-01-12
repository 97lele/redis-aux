package com.opensource.redisaux.bloomfilter.support.builder;

import com.opensource.redisaux.RedisAuxException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 21:28
 */
public final class BaseCondition {
    protected String keyPrefix;
    protected String keyName;
    public BaseCondition keyPrefix(String keyPrefix){
        this.keyPrefix=keyPrefix;
        return this;
    }
    public BaseCondition keyName(String keyName){
        this.keyName=keyName;
        return this;
    }
    public InnerInfo build(){
        if(Objects.isNull(keyName)){
            throw new RedisAuxException("key is null!");
        }
        return new InnerInfo(this);
    }
    public static BaseCondition of(){
        return new BaseCondition();
    }
}
