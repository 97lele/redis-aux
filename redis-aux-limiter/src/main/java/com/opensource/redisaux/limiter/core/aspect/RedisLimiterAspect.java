package com.opensource.redisaux.limiter.core.aspect;

import com.opensource.redisaux.common.CommonUtil;
import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.common.RedisAuxException;
import com.opensource.redisaux.limiter.annonations.group.LimiteExclude;
import com.opensource.redisaux.limiter.annonations.group.LimiteGroup;
import com.opensource.redisaux.limiter.annonations.normal.LimiterType;
import com.opensource.redisaux.limiter.autoconfigure.LimiterGroupService;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
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
public class RedisLimiterAspect {


    @Autowired
    private LimiterGroupService service;

    private final Map<String, Annotation> annotationMap;
    public final Map<Integer, BaseRateLimiter> rateLimiterMap;


    public RedisLimiterAspect(Map<Integer, BaseRateLimiter> rateLimiterMap) {
        this.annotationMap = new ConcurrentHashMap();
        this.rateLimiterMap = rateLimiterMap;
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
        BaseRateLimiter rateLimiter = rateLimiterMap.get(baseLimiter.mode());
        if (rateLimiter.canExecute(target, methodKey)) {
            return proceedingJoinPoint.proceed();
        } else {
            //否则执行失败逻辑
            BaseRateLimiter.KeyInfoNode keyInfoNode = BaseRateLimiter.keyInfoMap.get(methodKey);
            return executeFallBack(keyInfoNode.isPassArgs(), keyInfoNode.getFallBackMethod(), beanClass, method.getParameterTypes(), proceedingJoinPoint.getArgs(), proceedingJoinPoint.getTarget());
        }
    }

    @Pointcut("@within(com.opensource.redisaux.limiter.annonations.group.LimiteGroup)||@annotation(com.opensource.redisaux.limiter.annonations.group.LimiteGroup)")
    public void groupLimit() {

    }

    @Around("groupLimit()")
    public Object groupLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        //获取执行的方法
        Method method = signature.getMethod();
        //如果可以直接通过
        if (method.isAnnotationPresent(LimiteExclude.class)) {
            return proceedingJoinPoint.proceed();
        }
        Class<?> beanClass = proceedingJoinPoint.getTarget().getClass();
        String targetName = beanClass.getName();
        String methodKey = CommonUtil.getMethodKey(targetName, method);
        //获取所在类名
        LimiteGroupConfig limitGroupConfig = null;
        LimiteGroup annonation = null;
        if (method.isAnnotationPresent(LimiteGroup.class)) {
            annonation = method.getAnnotation(LimiteGroup.class);
        } else {
            annonation = beanClass.getAnnotation(LimiteGroup.class);
        }
        Object bean = proceedingJoinPoint.getTarget();
        limitGroupConfig = service.getLimiterConfig(annonation.groupId());
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String ipAddr = IpCheckUtil.getIpAddr(request);
        String requestURI = request.getRequestURI();
        BaseRateLimiter baseRateLimiter = rateLimiterMap.get(limitGroupConfig.getCurrentMode());
        int handle = service.handle(limitGroupConfig, ipAddr, requestURI, baseRateLimiter, methodKey);
        if(handle!= LimiterConstants.PASS){
            if(handle==LimiterConstants.TOOMUCHREQUEST){
                //否则执行失败逻辑
                return executeFallBack(annonation.passArgs(), annonation.fallback(),
                        beanClass, method.getParameterTypes(),
                        proceedingJoinPoint.getArgs(), bean);
            }else{
                return executeFallBack(annonation.passArgs(), limitGroupConfig.getBlackRuleFallback(),
                        beanClass, method.getParameterTypes(),
                        proceedingJoinPoint.getArgs(), bean);
            }
        }
        return proceedingJoinPoint.proceed();

    }

    private Object executeFallBack(Boolean passArgs, String methodStr, Class clazz, Class[] paramType, Object[] params, Object bean) throws Exception {
        if ("".equals(methodStr)) {
            throw new RedisAuxException("too much request or you are in black list");
        }
        Method fallBackMethod = passArgs ? clazz.getMethod(methodStr, paramType) : clazz.getMethod(methodStr);
        fallBackMethod.setAccessible(true);
        return passArgs ? fallBackMethod.invoke(bean, params) : fallBackMethod.invoke(bean);
    }
}