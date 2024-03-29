package com.xl.redisaux.limiter.core;

import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.common.api.WindowRateConfig;
import com.xl.redisaux.limiter.annonations.WindowLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2019/12/31 下午4:26
 * 根据滑动窗口实现,请看atuoConfiguration类注释
 */
@SuppressWarnings("unchecked")
public class WindowRateLimiter implements BaseRateLimiter {

    private RedisTemplate redisTemplate;

    private DefaultRedisScript<Boolean> redisScript;


    public WindowRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript = redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String methodKey) {
        WindowLimiter windowLimiter = (WindowLimiter) baseLimiter;
        List<String> keyList = Collections.singletonList(methodKey);
        return handleParam(keyList, windowLimiter.passCount(), windowLimiter.duringUnit(), windowLimiter.during());
    }

    @Override
    public Boolean canExecute(LimitGroupConfig limitGroup, String methodKey) {
        List<String> keyList = limitGroup.getWindowKeyName(methodKey);
        WindowRateConfig windowRateConfig = limitGroup.getWindowRateConfig();
        return handleParam(keyList, windowRateConfig.getPassCount(), windowRateConfig.getDuringUnit(), windowRateConfig.getDuring());
    }

    private Boolean handleParam(List<String> keyList, long value, TimeUnit timeUnit, long during) {
        long l = timeUnit.toMillis(during);
        long current = System.currentTimeMillis();
        long last = current - l;
        Object[] args = {current, last, value};
        Object res = redisTemplate.execute(redisScript, keyList, args);
        return (Boolean) res;
    }

}