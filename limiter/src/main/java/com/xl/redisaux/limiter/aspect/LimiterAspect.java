package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.common.exceptions.RedisAuxException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.Ordered;

import java.lang.reflect.Method;

/**
 * @author lulu
 * @Date 2020/7/18 9:38
 */
public interface LimiterAspect extends Ordered {



    default void limitPointCut() {

    }

    ;

    Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable;

    /**
     * 执行回调方法
     * @param passArgs
     * @param methodStr
     * @param clazz
     * @param paramType
     * @param params
     * @param bean
     * @return
     * @throws Exception
     */
    static Object executeFallBack(Boolean passArgs, String methodStr, Class<?> clazz, Class<?>[] paramType, Object[] params, Object bean) throws Exception {
        if (methodStr.isEmpty()) {
            throw new RedisAuxException("too much request");
        }
        Method fallBackMethod = passArgs ? clazz.getMethod(methodStr, paramType) : clazz.getMethod(methodStr);
        fallBackMethod.setAccessible(true);
        return passArgs ? fallBackMethod.invoke(bean, params) : fallBackMethod.invoke(bean);
    }
}
