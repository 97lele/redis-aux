package com.xl.redisaux.limiter.annonations;

import com.xl.redisaux.limiter.autoconfigure.normal.RedisLimiterRegistar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lulu
 * @Date 2020/2/15 13:09
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedisLimiterRegistar.class})
public @interface EnableLimiter {
    boolean enableGroup() default false;
    boolean delayConnect() default true;
}
