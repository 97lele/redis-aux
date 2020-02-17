package com.opensource.redisaux.limiter.annonations.group;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lulu
 * @Date 2020/2/16 14:51
 * 无须限流的方法，与作用在类上的limitGroup相互使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LimiteExclude {
}
