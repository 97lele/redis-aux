package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.limiter.annonations.normal.WindowLimiter;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import com.opensource.redisaux.limiter.core.group.config.WindowRateConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
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
        String methodName = windowLimiter.fallback();
        boolean passArgs = windowLimiter.passArgs();
        List<String> keyList = BaseRateLimiter.getKey(methodKey, methodName, passArgs);
        return handleParam(keyList, windowLimiter.passCount(), windowLimiter.duringUnit(), windowLimiter.during());
    }

    @Override
    public Boolean canExecute(LimiteGroupConfig limiteGroup, String methodKey) {
        List<String> keyList = limiteGroup.getWindowKeyName(methodKey);
        WindowRateConfig windowRateConfig = limiteGroup.getWindowRateConfig();
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