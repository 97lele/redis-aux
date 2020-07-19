package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.limiter.autoconfigure.RedisLimiterRegistar;
import com.xl.redisaux.transport.client.TcpHeartBeatClient;
import com.xl.redisaux.transport.config.TransportConfig;
import org.aspectj.lang.ProceedingJoinPoint;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author lulu
 * @Date 2020/7/18 9:38
 */
public interface LimiterAspect {


    AtomicBoolean HAS_REQUEST = new AtomicBoolean(false);

    default void limitPoinCut() {

    }

    ;

    Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable;

    Object executeFallBack(Boolean passArgs, String methodStr, Class clazz, Class[] paramType, Object[] params, Object bean) throws Exception;
}
