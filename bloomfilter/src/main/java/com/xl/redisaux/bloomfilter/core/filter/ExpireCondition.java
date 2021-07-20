package com.xl.redisaux.bloomfilter.core.filter;


import com.xl.redisaux.common.exceptions.RedisAuxException;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 21:26
 */
public final class ExpireCondition extends BaseCondition<ExpireCondition>{
    protected Long timeout;
    protected TimeUnit timeUnit;
    protected Boolean local;


    public ExpireCondition timeout(Long timeuot) {
        this.timeout = timeuot;
        return this;
    }

    public ExpireCondition timeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    public ExpireCondition local(Boolean local){
        this.local=local;
        return this;
    }

   public InnerInfo build() {
        if (keyName == null) {
            throw new RedisAuxException("key is null!");
        }
        this.timeUnit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        this.timeout = timeout == null ? -1L : timeout;
        return new InnerInfo(this);
    }

    public static ExpireCondition create() {
        return new ExpireCondition();
    }


    @Override
    public String toString() {
        return "ExpireCondition{" +
                "keyPrefix='" + keyPrefix + '\'' +
                ", keyName='" + keyName + '\'' +
                ", timeout=" + timeout +
                ", timeUnit=" + timeUnit +
                ", local=" + local +
                '}';
    }
}
