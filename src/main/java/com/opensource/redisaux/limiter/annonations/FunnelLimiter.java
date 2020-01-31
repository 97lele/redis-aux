package com.opensource.redisaux.limiter.annonations;

import com.opensource.redisaux.limiter.core.BaseRateLimiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/4 上午8:26
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@LimiterType(mode = BaseRateLimiter.FUNNEL_LIMITER)
public @interface FunnelLimiter {

    /**
     * 漏斗容量
     * @return
     */
    double capacity();
    /**
     *每秒漏出的速率
     * @return
     */
    double passRate();
    /**
     *时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    /**
     *每次请求所需加的水量
     * @return
     */
    double addWater();

    String fallback() default "";

    boolean passArgs() default false;

}