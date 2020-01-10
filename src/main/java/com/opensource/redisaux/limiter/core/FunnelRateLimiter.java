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
public class FunnelRateLimiter implements RateLimiter {
    private RedisTemplate redisTemplate;
    private DefaultRedisScript redisScript;


    public FunnelRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript = redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String key) {
        FunnelLimiter funnelLimiter = (FunnelLimiter) baseLimiter;
        TimeUnit timeUnit = funnelLimiter.timeUnit();
        double capacity = funnelLimiter.capacity();
        double need = funnelLimiter.addWater();
        double rate = funnelLimiter.passRate();
        long l = timeUnit.toMillis(1);
        double millRate = rate / l;
        List<String> keyList = RateLimiter.getKeyAndPutFailStrategyIfAbsent(key, funnelLimiter.failStrategy(),funnelLimiter.msg());
        return (Boolean) redisTemplate.execute(redisScript, keyList, new Object[]{capacity, millRate, need, Double.valueOf(System.currentTimeMillis())});
    }


}