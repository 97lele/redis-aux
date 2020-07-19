package com.xl.redisaux.limiter.annonations;

import com.xl.redisaux.common.consts.LimiterConstants;

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
@LimiterType(mode = LimiterConstants.TOKEN_LIMITER)
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
    double tokenRate();

    /**
     * 速率时间单位，默认秒
     *
     * @return
     */
    TimeUnit tokenRateUnit() default TimeUnit.SECONDS;

    /**
     * 每次请求所需要的令牌数
     *
     * @return
     */
    double requestNeed() default 1;

    double initToken() default 0;


    String fallback() default "";

    boolean passArgs() default false;

}