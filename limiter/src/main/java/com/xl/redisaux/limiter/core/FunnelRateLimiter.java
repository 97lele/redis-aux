package com.xl.redisaux.limiter.core;

import com.xl.redisaux.common.api.FunnelRateConfig;
import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.limiter.annonations.FunnelLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/4 上午8:12
 */
@SuppressWarnings("unchecked")
public class FunnelRateLimiter implements BaseRateLimiter {
    private RedisTemplate redisTemplate;
    private DefaultRedisScript redisScript;


    public FunnelRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript = redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String methodKey) {
        FunnelLimiter funnelLimiter = (FunnelLimiter) baseLimiter;
        TimeUnit timeUnit = funnelLimiter.funnelRateUnit();
        double capacity = funnelLimiter.capacity();
        double need = funnelLimiter.requestNeed();
        double rate = funnelLimiter.funnelRate();
        List<String> keyList = Collections.singletonList(methodKey);
        return handleParam(keyList, capacity, need, rate, timeUnit);
    }

    @Override
    public Boolean canExecute(LimitGroupConfig limitGroup, String methodKey) {
        FunnelRateConfig funnelRateConfig = limitGroup.getFunnelRateConfig();
        double funnelCapacity = funnelRateConfig.getCapacity();
        double funnelRatePerTimeUnit = funnelRateConfig.getFunnelRate();
        double funnelRequestNeed = funnelRateConfig.getRequestNeed();
        return handleParam(limitGroup.getFunnelKeyName(methodKey), funnelCapacity, funnelRequestNeed, funnelRatePerTimeUnit, funnelRateConfig.getFunnelRateUnit());
    }

    private Boolean handleParam(List<String> keyList, double capacity, double need, double rate, TimeUnit timeUnit) {
        double millRate = rate / timeUnit.toMillis(1);
        Object res=redisTemplate.execute(redisScript, keyList, new Object[]{capacity, millRate, need, Double.valueOf(System.currentTimeMillis())});
        return (Boolean)res;
    }


}