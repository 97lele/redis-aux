package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.common.utils.IpCheckUtil;
import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.common.utils.NamedThreadFactory;
import com.xl.redisaux.limiter.annonations.LimiteExclude;
import com.xl.redisaux.limiter.annonations.LimiteGroup;
import com.xl.redisaux.limiter.autoconfigure.LimiterGroupService;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterAutoConfiguration;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterRegistar;
import com.xl.redisaux.limiter.core.BaseRateLimiter;
import com.xl.redisaux.limiter.config.LimiteGroupConfig;
import com.xl.redisaux.transport.client.TcpHeartBeatClient;
import com.xl.redisaux.transport.config.TransportConfig;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lulu
 * @Date 2020/2/17 20:13
 */
@Aspect
public class GroupLimiterAspect implements LimiterAspect{

    @Autowired
    private LimiterGroupService service;

    private ExecutorService executor= Executors.newSingleThreadExecutor(new NamedThreadFactory("client-heartBeat-run",true));

    @Override
    @Pointcut("@within(com.xl.redisaux.limiter.annonations.LimiteGroup)||@annotation(com.xl.redisaux.limiter.annonations.LimiteGroup)")
    public void limitPoinCut() {

    }

    @Override
    @Around("limitPoinCut()")
    public Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (RedisLimiterRegistar.connectConsole.get() && !LimiterAspect.HAS_REQUEST.get()) {
            LimiterAspect.HAS_REQUEST.set(true);
            TcpHeartBeatClient client=new TcpHeartBeatClient(TransportConfig.get(TransportConfig.CONSOLE_IP),TransportConfig.get(TransportConfig.CONSOLE_PORT,Integer::valueOf));
            executor.submit(()->{
                try {
                    client.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }
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
        String groupId = annonation.groupId();
        Object bean = proceedingJoinPoint.getTarget();
        limitGroupConfig = service.getLimiterConfig(groupId);
        if (limitGroupConfig == null) {
            return proceedingJoinPoint.proceed();
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String ipAddr = limitGroupConfig.isEnableBlackList() || limitGroupConfig.isEnableWhiteList() ? IpCheckUtil.getIpAddr(request) : null;
        String requestURI = request.getRequestURI();
        BaseRateLimiter baseRateLimiter = RedisLimiterAutoConfiguration.rateLimiterMap.get(limitGroupConfig.getCurrentMode());
        LimiteGroupConfig point = limitGroupConfig;
        //逻辑链处理
        Integer handle = service.handle(point, ipAddr, requestURI, baseRateLimiter, methodKey);

        if (handle != LimiterConstants.PASS) {
            //是否计数
            if (limitGroupConfig.isEnableQpsCount()) {
                service.updateCount(false, point);
            }
            String methodStr = annonation.fallback();
            if (handle == LimiterConstants.WRONGPREFIX && limitGroupConfig.getUrlFallBack() != null) {
                methodStr = limitGroupConfig.getUrlFallBack();
            }
            if (handle == LimiterConstants.INBLACKLIST && limitGroupConfig.getBlackRuleFallback() != null) {
                methodStr = limitGroupConfig.getBlackRuleFallback();
            }
            return this.executeFallBack(annonation.passArgs(), methodStr, beanClass, method.getParameterTypes(), proceedingJoinPoint.getArgs(), bean);
        } else {
            if (limitGroupConfig.isEnableQpsCount()) {
                service.updateCount(true, point);
            }
            return proceedingJoinPoint.proceed();
        }


    }

    @Override
    public Object executeFallBack(Boolean passArgs, String methodStr, Class clazz, Class[] paramType, Object[] params, Object bean) throws Exception {
        if ("".equals(methodStr)) {
            throw new RedisAuxException("too much request");
        }
        Method fallBackMethod = passArgs ? clazz.getMethod(methodStr, paramType) : clazz.getMethod(methodStr);
        fallBackMethod.setAccessible(true);
        return passArgs ? fallBackMethod.invoke(bean, params) : fallBackMethod.invoke(bean);
    }
}
