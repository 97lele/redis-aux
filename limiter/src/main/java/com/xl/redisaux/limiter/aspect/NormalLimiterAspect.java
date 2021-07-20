package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.limiter.annonations.LimiterType;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterAutoConfiguration;
import com.xl.redisaux.limiter.core.BaseRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
@Slf4j
public class NormalLimiterAspect implements LimiterAspect {

    private final Map<String, Annotation> annotationMap;

    private static ThreadLocal<Boolean> hasExecuted = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public NormalLimiterAspect() {
        this.annotationMap = new ConcurrentHashMap<>();
    }


    @Override
    @Pointcut("@annotation(com.xl.redisaux.limiter.annonations.TokenLimiter)||@annotation(com.xl.redisaux.limiter.annonations.WindowLimiter)||@annotation(com.xl.redisaux.limiter.annonations.FunnelLimiter)")
    public void limitPointCut() {

    }


    @Override
    @Around("limitPointCut()")
    public Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (!hasExecuted.get()) {
            hasExecuted.set(true);
            MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
            Class<?> beanClass = proceedingJoinPoint.getTarget().getClass();
            //获取所在类名
            //获取执行的方法
            Method method = signature.getMethod();
            String methodKey = CommonUtil.getMethodKey(beanClass.getName(), method);
            //该注解用于获取对应限流器
            LimiterType baseLimiter = null;
            Annotation target = null;
            if ((target = annotationMap.get(methodKey)) == null) {
                //找出限流器并且把对应的注解存到map里面
                Annotation[] annotations = method.getAnnotations();
                for (Annotation annotation : annotations) {
                    Class<? extends Annotation> annotationType = annotation.annotationType();
                    if (annotationType.isAnnotationPresent(LimiterType.class)) {
                        target = annotation;
                        annotationMap.put(methodKey, target);
                        annotationMap.put(methodKey+"#",AnnotatedElementUtils.getMergedAnnotation(method,LimiterType.class));
                        break;
                    }
                }
            }
            boolean canPass = true;
            if (target != null) {
                baseLimiter = (LimiterType) annotationMap.get(methodKey+"#");
                BaseRateLimiter rateLimiter = RedisLimiterAutoConfiguration.RATE_LIMITER_MAP.get(baseLimiter.mode());
                try {
                    canPass = rateLimiter.canExecute(target, methodKey);
                } catch (Exception e) {
                    log.error("limit error:", e);
                }
            }
            if (!canPass) {
                hasExecuted.remove();
                return LimiterAspect.executeFallBack(baseLimiter.passArgs(), baseLimiter.fallback(), beanClass, method.getParameterTypes(), proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget());
            }
        } else {
            hasExecuted.remove();
        }
        return proceedingJoinPoint.proceed();

    }
}