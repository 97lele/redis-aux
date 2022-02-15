package com.xl.redisaux.bloomfilter.annonations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface BloomFilterProperty {
    //错误率
    double fpp() default 0.03;
    //预计插入数
    long exceptionInsert() default 1000;
    //键
    String key() default "";

    long timeout() default -1L;

    TimeUnit timeUnit() default TimeUnit.SECONDS;
    //是否本地数组
    boolean local() default false;
}