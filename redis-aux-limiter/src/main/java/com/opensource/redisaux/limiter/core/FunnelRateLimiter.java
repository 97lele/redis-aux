package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.limiter.annonations.normal.FunnelLimiter;
import com.opensource.redisaux.limiter.core.group.config.FunnelRateConfig;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/4 上午8:12
 */
@SuppressWarnings("unchecked")
public class FunnelRateLimiter extends BaseRateLimiter {
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
        String methodName = funnelLimiter.fallback();
        boolean passArgs = funnelLimiter.passArgs();
        List<String> keyList = BaseRateLimiter.getKey(methodKey, methodName, passArgs);
        return handleParam(keyList, capacity, need, rate, timeUnit);
    }

    @Override
    public Boolean canExecute(LimiteGroupConfig limiteGroup, String methodKey) {
        FunnelRateConfig funnelRateConfig = limiteGroup.getFunnelRateConfig();
        double funnelCapacity = funnelRateConfig.getCapacity();
        double funnelRatePerTimeUnit = funnelRateConfig.getFunnelRate();
        double funnelRequestNeed = funnelRateConfig.getRequestNeed();
        return handleParam(limiteGroup.getFunnelKeyName(methodKey), funnelCapacity, funnelRequestNeed, funnelRatePerTimeUnit, funnelRateConfig.getFunnelRateUnit());
    }

    private Boolean handleParam(List<String> keyList, double capacity, double need, double rate, TimeUnit timeUnit) {
        double millRate = rate / timeUnit.toMillis(1);
        Object res=redisTemplate.execute(redisScript, keyList, new Object[]{capacity, millRate, need, Double.valueOf(System.currentTimeMillis())});
        return (Boolean)res;
    }


}