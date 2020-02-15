package com.opensource.redisaux.bloomfilter.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lulu
 * @Date 2020/2/15 13:08
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RedisBloomFilterRegistar.class})
public @interface EnableBloomFilter {
    boolean transaction() default false;
    String[] bloomFilterPath() default "";

}
