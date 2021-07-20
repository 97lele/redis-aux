package com.xl.redisaux.limiter.annonations;


import com.xl.redisaux.common.consts.LimiterConstants;
import org.springframework.core.annotation.AliasFor;

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

    @AliasFor(annotation = LimiterType.class,attribute = "fallback")
    String fallback() default "";

    @AliasFor(annotation = LimiterType.class,attribute = "passArgs")
    boolean passArgs() default false;


}