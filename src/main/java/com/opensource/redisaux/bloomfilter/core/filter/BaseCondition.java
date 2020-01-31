package com.opensource.redisaux.bloomfilter.core.filter;

import com.opensource.redisaux.RedisAuxException;


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
     InnerInfo build(){
        if(keyName==null){
            throw new RedisAuxException("key is null!");
        }
        return new InnerInfo(this);
    }
    public static BaseCondition create(){
        return new BaseCondition();
    }
}
