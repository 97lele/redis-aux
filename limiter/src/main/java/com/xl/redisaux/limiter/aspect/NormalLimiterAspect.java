package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.limiter.annonations.LimiterType;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterAutoConfiguration;
import com.xl.redisaux.limiter.core.BaseRateLimiter;
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
public class NormalLimiterAspect implements LimiterAspect{

    @Autowired
    @Qualifier(LimiterConstants.LIMITER)
    private RedisTemplate redisTemplate;

    private final Map<String, Annotation> annotationMap;


    public NormalLimiterAspect() {
        this.annotationMap = new ConcurrentHashMap();

    }


    @Override
    @Pointcut("@annotation(com.xl.redisaux.limiter.annonations.TokenLimiter)||@annotation(com.xl.redisaux.limiter.annonations.WindowLimiter)||@annotation(com.xl.redisaux.limiter.annonations.FunnelLimiter)")
    public void limitPoinCut() {

    }


    @Override
    @Around("limitPoinCut()")
    public Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

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



    @Override
    public  Object executeFallBack(Boolean passArgs, String methodStr, Class clazz, Class[] paramType, Object[] params, Object bean) throws Exception {
        if ("".equals(methodStr)) {
            throw new RedisAuxException("too much request");
        }
        Method fallBackMethod = passArgs ? clazz.getMethod(methodStr, paramType) : clazz.getMethod(methodStr);
        fallBackMethod.setAccessible(true);
        return passArgs ? fallBackMethod.invoke(bean, params) : fallBackMethod.invoke(bean);
    }
}