package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.limiter.annonations.WindowLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2019/12/31 下午4:26
 * 根据滑动窗口实现,请看atuoConfiguration类注释
 */
@SuppressWarnings("unchecked")
public class WindowRateLimiter extends BaseRateLimiter {

    private RedisTemplate redisTemplate;

    private DefaultRedisScript<Boolean> redisScript;



    public WindowRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript=redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String methodKey) {
        WindowLimiter windowLimiter=(WindowLimiter)baseLimiter;
        int i = windowLimiter.during();
        TimeUnit timeUnit = windowLimiter.timeUnit();
        String methodName=windowLimiter.fallback();
        boolean passArgs=windowLimiter.passArgs();
        //转为毫秒实现
        long l = timeUnit.toMillis(i);
        long value = windowLimiter.value();
        //当前时间戳
        long current = System.currentTimeMillis();
        //上一个截止的时间戳
        long last = current - l;
        Object[] args = {current ,last,value};
        Object execute = redisTemplate.execute(redisScript, BaseRateLimiter.getKey(methodKey,methodName,passArgs), args);
        return (Boolean) execute;
    }

}