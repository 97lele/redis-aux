package com.xl.redisaux.bloomfilter.core.filter;


import com.xl.redisaux.common.exceptions.RedisAuxException;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 20:48
 */
public final class AddCondition extends BaseCondition<AddCondition>{
    protected Double fpp;
    protected Long exceptionInsert;
    protected Long timeout;
    protected TimeUnit timeUnit;
    protected ExpireCondition expireCondition;
    protected Boolean local;


    public AddCondition fpp(Double fpp) {
        this.fpp = fpp;
        return this;
    }

    public AddCondition exceptionInsert(Long exceptionInsert) {
        this.exceptionInsert = exceptionInsert;
        return this;
    }


    public AddCondition timeout(Long timeuot) {
        this.timeout = timeuot;
        return this;
    }

    public AddCondition timeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }



    public AddCondition local(Boolean local) {
        this.local = local;
        return this;
    }

  public  InnerInfo build() {
        if (keyName == null) {
            throw new RedisAuxException("key is null!");
        }
        this.fpp = fpp == null ? 0.03 : fpp;
        this.exceptionInsert = exceptionInsert == null ? 1000L : exceptionInsert;
        this.timeUnit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        this.timeout = timeout == null ? -1L : timeout;
        this.local=local==null?false:local;
        return new InnerInfo(this);

    }

    public ExpireCondition toExpireCondition() {
        if (this.expireCondition == null) {
            this.expireCondition = ExpireCondition.create().keyName(keyName).keyPrefix(keyPrefix).timeout(timeout).timeUnit(timeUnit).local(local);
        }
        return this.expireCondition;
    }

    public static AddCondition create() {
        return new AddCondition();
    }

    protected void clear() {
        this.expireCondition = null;
    }

    public Double getFpp() {
        return fpp;
    }

    public Long getExceptionInsert() {
        return exceptionInsert;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyName() {
        return keyName;
    }

    public Long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }


    public Boolean getLocal() {
        return local;
    }

    @Override
    public String toString() {
        return "AddCondition{" +
                "fpp=" + fpp +
                ", exceptionInsert=" + exceptionInsert +
                ", keyPrefix='" + keyPrefix + '\'' +
                ", keyName='" + keyName + '\'' +
                ", timeout=" + timeout +
                ", timeUnit=" + timeUnit +
                ", local=" + local +
                '}';
    }
}
