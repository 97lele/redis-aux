package com.xl.redisaux.bloomfilter.core.filter;


import com.xl.redisaux.common.exceptions.RedisAuxException;

/**
 * @author lulu
 * @Date 2020/1/11 21:28
 */
public  class BaseCondition<T extends BaseCondition> {
    protected String keyPrefix;
    protected String keyName;

    public T keyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return (T)this;
    }

    public T keyName(String keyName) {
        this.keyName = keyName;
        return (T)this;
    }

   protected InnerInfo build() {
        if (keyName == null) {
            throw new RedisAuxException("key is null!");
        }
        return new InnerInfo(this);
    }

    public static BaseCondition create() {
        return new BaseCondition();
    }


    @Override
    public String toString() {
        return "BaseCondition{" +
                "keyPrefix='" + keyPrefix + '\'' +
                ", keyName='" + keyName + '\'' +
                '}';
    }
}
