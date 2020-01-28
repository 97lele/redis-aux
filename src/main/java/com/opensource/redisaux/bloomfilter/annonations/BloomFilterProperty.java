package com.opensource.redisaux.bloomfilter.annonations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BloomFilterProperty {
    double fpp() default 0.03;
    long exceptionInsert() default 1000;
    String key() default "";
    long timeout() default -1L;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    boolean enableGrow() default false;
    //当redis布隆过滤器的bitcount大于预计插入数量所计算得出的bitcount的0.7倍时，新增一个
    double growRate() default 0.7;
}