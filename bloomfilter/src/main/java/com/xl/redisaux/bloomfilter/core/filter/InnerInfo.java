package com.xl.redisaux.bloomfilter.core.filter;

import java.util.concurrent.TimeUnit;

class InnerInfo {
    private double fpp;
    private long exceptionInsert;
    private String keyPrefix;
    private String keyName;
    private long timeout;
    private TimeUnit timeUnit;
    private boolean local;

    public InnerInfo(AddCondition addCondition) {
        this.fpp = addCondition.fpp;
        this.exceptionInsert = addCondition.exceptionInsert;
        this.keyPrefix = addCondition.keyPrefix;
        this.keyName = addCondition.keyName;
        this.timeout = addCondition.timeout;
        this.timeUnit = addCondition.timeUnit;
        this.local=addCondition.local;
    }

    public InnerInfo(ExpireCondition expireCondition) {
        this.keyPrefix = expireCondition.keyPrefix;
        this.keyName = expireCondition.keyName;
        this.timeout = expireCondition.timeout;
        this.timeUnit = expireCondition.timeUnit;
    }

    public InnerInfo(BaseCondition condition) {
        this.keyPrefix = condition.keyPrefix;
        this.keyName = condition.keyName;
    }

    public double getFpp() {
        return fpp;
    }

    public long getExceptionInsert() {
        return exceptionInsert;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyName() {
        return keyName;
    }

    public long getTimeout() {
        return timeout;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public boolean isLocal(){return local;}
}