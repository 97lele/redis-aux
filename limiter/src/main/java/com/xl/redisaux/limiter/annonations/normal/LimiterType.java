package com.xl.redisaux.limiter.annonations.normal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LimiterType {
    /**
     * 模式，用于去相应的map里面寻找对应的limiter
     *
     * @return
     */
    int mode();
}