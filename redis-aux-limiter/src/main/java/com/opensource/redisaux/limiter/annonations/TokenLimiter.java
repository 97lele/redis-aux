package com.opensource.redisaux.limiter.annonations;

import com.opensource.redisaux.limiter.core.BaseRateLimiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:44
 * 令牌桶
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@LimiterType(mode = BaseRateLimiter.TOKEN_LIMITER)
public @interface TokenLimiter {

    /**
     * 令牌桶容量
     *
     * @return
     */
    double capacity();

    /**
     * 令牌生成速率
     *
     * @return
     */
    double rate();

    /**
     * 速率时间单位，默认秒
     *
     * @return
     */
    TimeUnit rateUnit() default TimeUnit.SECONDS;

    /**
     * 每次请求所需要的令牌数
     *
     * @return
     */
    double need();

    /**
     * 是否阻塞等待
     *
     * @return
     */
    boolean isAbort() default false;

    /**
     * 阻塞超时时间
     *
     * @return
     */
    int timeout() default -1;

    /**
     * 单位，默认毫秒
     *
     * @return
     */
    TimeUnit timeoutUnit() default TimeUnit.MILLISECONDS;

    String fallback() default "";

    boolean passArgs() default false;

}