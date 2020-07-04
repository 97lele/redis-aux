package com.opensource.redisaux.limiter.annonations.normal;


import com.opensource.redisaux.common.consts.LimiterConstants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@LimiterType(mode = LimiterConstants.WINDOW_LIMITER)
public @interface WindowLimiter {
    /**
     * 持续时间，窗口间隔
     *
     * @return
     */
    int during() default 1;

    TimeUnit duringUnit() default TimeUnit.SECONDS;

    /**
     * 通过的请求数
     *
     * @return
     */
    long passCount();

    String fallback() default "";

    boolean passArgs() default false;


}