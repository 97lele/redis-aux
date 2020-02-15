package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.limiter.annonations.FunnelLimiter;
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
        TimeUnit timeUnit = funnelLimiter.timeUnit();
        double capacity = funnelLimiter.capacity();
        double need = funnelLimiter.addWater();
        double rate = funnelLimiter.passRate();
        long l = timeUnit.toMillis(1);
        double millRate = rate / l;
        String methodName = funnelLimiter.fallback();
        boolean passArgs = funnelLimiter.passArgs();
        List<String> keyList = BaseRateLimiter.getKey(methodKey, methodName, passArgs);
        return (Boolean) redisTemplate.execute(redisScript, keyList, new Object[]{capacity, millRate, need, Double.valueOf(System.currentTimeMillis())});
    }


}