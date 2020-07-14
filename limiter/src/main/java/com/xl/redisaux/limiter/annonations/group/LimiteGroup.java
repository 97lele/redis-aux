package com.xl.redisaux.limiter.annonations.group;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author lulu
 * @Date 2020/2/15 22:28
 * 这里的group需要用户提前配置好
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface LimiteGroup {
    String groupId();
    String fallback() default "";
    boolean passArgs() default false;
}
