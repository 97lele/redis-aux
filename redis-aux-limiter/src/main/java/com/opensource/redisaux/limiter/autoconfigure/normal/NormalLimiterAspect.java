package com.opensource.redisaux.limiter.autoconfigure.normal;

import com.opensource.redisaux.common.CommonUtil;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.common.RedisAuxException;
import com.opensource.redisaux.limiter.annonations.normal.LimiterType;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import io.lettuce.core.RedisException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/2 下午5:12
 */
@SuppressWarnings("unchecked")
@Aspect
public class NormalLimiterAspect {

    @Autowired
    @Qualifier(LimiterConstants.LIMITER)
    private RedisTemplate redisTemplate;

    private final Map<String, Annotation> annotationMap;


    public NormalLimiterAspect(Map<Integer, BaseRateLimiter> rateLimiterMap) {
        this.annotationMap = new ConcurrentHashMap();

    }


    @Pointcut("@annotation(com.opensource.redisaux.limiter.annonations.normal.TokenLimiter)||@annotation(com.opensource.redisaux.limiter.annonations.normal.WindowLimiter)||@annotation(com.opensource.redisaux.limiter.annonations.normal.FunnelLimiter)")
    public void limitPoint() {

    }


    @Around("limitPoint()")
    public Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Class<?> beanClass = proceedingJoinPoint.getTarget().getClass();
        //获取所在类名
        String targetName = beanClass.getName();
        //获取执行的方法
        Method method = signature.getMethod();
        String methodKey = CommonUtil.getMethodKey(targetName, method);
        //该注解用于获取对应限流器
        LimiterType baseLimiter = null;
        Annotation target = null;
        if ((target = annotationMap.get(methodKey)) == null) {
            //找出限流器并且把对应的注解存到map里面
            Annotation[] annotations = signature.getMethod().getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().isAnnotationPresent(LimiterType.class)) {
                    target = annotation;
                    annotationMap.put(methodKey, target);
                    break;
                }
            }
        }
        baseLimiter = target.annotationType().getAnnotation(LimiterType.class);

        BaseRateLimiter rateLimiter = RedisLimiterAutoConfiguration.rateLimiterMap.get(baseLimiter.mode());
        Boolean b=true;
        try{
            b=rateLimiter.canExecute(target, methodKey);
        }catch (RedisException e){
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
        if (b) {
            return proceedingJoinPoint.proceed();
        } else {
            //否则执行失败逻辑
            BaseRateLimiter.KeyInfoNode keyInfoNode = BaseRateLimiter.keyInfoMap.get(methodKey);
            return executeFallBack(keyInfoNode.isPassArgs(), keyInfoNode.getFallBackMethod(), beanClass, method.getParameterTypes(), proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget());
        }


    }



    protected static Object executeFallBack(Boolean passArgs, String methodStr, Class clazz, Class[] paramType, Object[] params, Object bean) throws Exception {
        if ("".equals(methodStr)) {
            throw new RedisAuxException("too much request");
        }
        Method fallBackMethod = passArgs ? clazz.getMethod(methodStr, paramType) : clazz.getMethod(methodStr);
        fallBackMethod.setAccessible(true);
        return passArgs ? fallBackMethod.invoke(bean, params) : fallBackMethod.invoke(bean);
    }
}