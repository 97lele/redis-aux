package com.opensource.redisaux;

import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilterRegistar;
import com.opensource.redisaux.limiter.autoconfigure.RedisLimiterRegistar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lulu
 * @Date 2020/1/11 1:57
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedisBloomFilterRegistar.class, RedisLimiterRegistar.class})
public @interface EnableRedisAux {
    String[] bloomFilterPath() default "";
    boolean enableLimit() default false;
    boolean transaction() default false;

}