package com.opensource.redisaux.bloomfilter.core.filter;


import com.opensource.redisaux.common.RedisAuxException;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/1/11 20:48
 */
public final class AddCondition {
    protected Double fpp;
    protected Long exceptionInsert;
    protected String keyPrefix;
    protected String keyName;
    protected Long timeout;
    protected TimeUnit timeUnit;
    protected Boolean enableGrow;
    protected Double growRate;
    protected BaseCondition baseCondition;
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

    public AddCondition keyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
    }

    public AddCondition keyName(String keyName) {
        this.keyName = keyName;
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

    public AddCondition enableGrow(Boolean enableGrow) {
        this.enableGrow = enableGrow;
        return this;
    }

    public AddCondition growRate(Double growRate) {
        this.growRate = growRate;
        return this;
    }

    public AddCondition local(Boolean local) {
        this.local = local;
        return this;
    }

    InnerInfo build() {
        if (keyName == null) {
            throw new RedisAuxException("key is null!");
        }
        this.fpp = fpp == null ? 0.03 : fpp;
        this.exceptionInsert = exceptionInsert == null ? 1000L : exceptionInsert;
        this.timeUnit = timeUnit == null ? TimeUnit.SECONDS : timeUnit;
        this.timeout = timeout == null ? -1L : timeout;
        this.growRate = growRate == null ? 0.7 : growRate;
        //默认不开启自增
        this.enableGrow = enableGrow == null ? false : enableGrow;
        this.local=local==null?false:local;
        return new InnerInfo(this);

    }

    public BaseCondition toBaseCondition() {
        if (this.baseCondition == null) {
            this.baseCondition = BaseCondition.create().keyName(keyName).keyPrefix(keyPrefix);
        }
        return this.baseCondition;
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
        this.baseCondition = null;
    }

}
