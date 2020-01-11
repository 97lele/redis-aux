package com.opensource.redisaux.bloomfilter.support.builder;

import java.util.concurrent.TimeUnit;

public class InnerInfo {
    private Double fpp;
    private Long exceptionInsert;
    private String keyPrefix;
    private String keyName;
    private Long timeout;
    private TimeUnit timeUnit;

    public InnerInfo(AddCondition addCondition) {
        this.fpp = addCondition.fpp;
        this.exceptionInsert = addCondition.exceptionInsert;
        this.keyPrefix = addCondition.keyPrefix;
        this.keyName = addCondition.keyName;
        this.timeout = addCondition.timeout;
        this.timeUnit = addCondition.timeUnit;
    }
    public InnerInfo(ExpireCondition expireCondition){
        this.keyPrefix = expireCondition.keyPrefix;
        this.keyName = expireCondition.keyName;
        this.timeout = expireCondition.timeout;
        this.timeUnit = expireCondition.timeUnit;
    }
    public InnerInfo(BaseCondition condition){
        this.keyPrefix = condition.keyPrefix;
        this.keyName = condition.keyName;
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
}