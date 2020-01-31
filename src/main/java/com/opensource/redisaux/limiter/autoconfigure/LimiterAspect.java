package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.limiter.annonations.LimiterType;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/2 下午5:12
 */
@Aspect
public class LimiterAspect  {


    private final Map<Integer, BaseRateLimiter> rateLimiterMap;

    private final Map<String, Annotation> annotationMap;


    public LimiterAspect(Map<Integer, BaseRateLimiter> rateLimiterMap
    ) {
        this.rateLimiterMap = rateLimiterMap;
        this.annotationMap = new ConcurrentHashMap<>();
    }


    @Pointcut("@annotation(com.opensource.redisaux.limiter.annonations.TokenLimiter)||@annotation(com.opensource.redisaux.limiter.annonations.WindowLimiter)||@annotation(com.opensource.redisaux.limiter.annonations.FunnelLimiter)")
    public void limitPoint() {

    }


    @Around("limitPoint()")
    public Object doAroundAdvice(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
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
        BaseRateLimiter rateLimiter = rateLimiterMap.get(baseLimiter.mode());
        if (rateLimiter.canExecute(target, methodKey)) {
            return proceedingJoinPoint.proceed();
        } else {
            //否则执行失败逻辑
            Object bean =proceedingJoinPoint.getTarget();
            BaseRateLimiter.KeyInfoNode keyInfoNode = BaseRateLimiter.keyInfoMap.get(methodKey);
            String fallBackMethodStr = keyInfoNode.getFallBackMethod();
            if ("".equals(fallBackMethodStr)) {
                return "too much request";
            }

           Method fallBackMethod= keyInfoNode.isPassArgs()?
                   beanClass.getMethod(fallBackMethodStr,method.getParameterTypes()):
                   beanClass.getMethod(fallBackMethodStr);
            fallBackMethod.setAccessible(true);
           return keyInfoNode.isPassArgs()?fallBackMethod.invoke(bean,proceedingJoinPoint.getArgs()):fallBackMethod.invoke(bean);
        }
    }



}