package com.xl.redisaux.limiter.aspect;

import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.common.utils.IpCheckUtil;
import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.limiter.annonations.LimiteExclude;
import com.xl.redisaux.limiter.annonations.LimitGroup;
import com.xl.redisaux.limiter.component.LimiterGroupService;
import com.xl.redisaux.limiter.autoconfigure.RedisLimiterAutoConfiguration;
import com.xl.redisaux.limiter.core.BaseRateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.util.Pair;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * @author lulu
 * @Date 2020/2/17 20:13
 */
@Aspect
public class GroupLimiterAspect implements LimiterAspect {

    @Resource
    private LimiterGroupService service;


    @Override
    @Pointcut("@within(com.xl.redisaux.limiter.annonations.LimitGroup)||@annotation(com.xl.redisaux.limiter.annonations.LimitGroup)")
    public void limitPointCut() {

    }

    @Override
    @Around("limitPointCut()")
    public Object methodLimit(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
            MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
            //获取执行的方法
            Method method = signature.getMethod();
            //如果可以直接通过
            if (method.isAnnotationPresent(LimiteExclude.class)) {
                return proceedingJoinPoint.proceed();
            }
            Class<?> beanClass = proceedingJoinPoint.getTarget().getClass();
            String methodKey = CommonUtil.getMethodKey(beanClass.getName(), method);
            //获取所在类名
            LimitGroupConfig limitGroupConfig = null;
            LimitGroup group = null;
            if (method.isAnnotationPresent(LimitGroup.class)) {
                group = method.getAnnotation(LimitGroup.class);
            } else {
                group = beanClass.getAnnotation(LimitGroup.class);
            }
            String groupId = group.groupId();
            Object bean = proceedingJoinPoint.getTarget();
            limitGroupConfig = service.getLimiterConfig(groupId);
            if (limitGroupConfig == null) {
                return proceedingJoinPoint.proceed();
            }
            //获取ip地址和访问url
            Pair<String, String> ipAndAdder = getIpAndRequestUrl(limitGroupConfig);
            //获取限流器
            BaseRateLimiter baseRateLimiter = RedisLimiterAutoConfiguration.RATE_LIMITER_MAP.get(limitGroupConfig.getCurrentMode());
            //逻辑链处理
            int handleResult = service.handle(limitGroupConfig, ipAndAdder.getFirst(), ipAndAdder.getSecond(), baseRateLimiter, methodKey);
            //是否计数
            boolean enableQpsCount = limitGroupConfig.isEnableQpsCount();
            boolean pass = handleResult == LimiterConstants.PASS;
            updateCount(enableQpsCount, pass, limitGroupConfig);
            if (!pass) {
                //触发异常方法，清除
                String methodStr = group.fallback();
                if (handleResult == LimiterConstants.WRONGPREFIX && limitGroupConfig.getUrlFallBack() != null) {
                    methodStr = limitGroupConfig.getUrlFallBack();
                }
                if (handleResult == LimiterConstants.INBLACKLIST && limitGroupConfig.getBlackRuleFallback() != null) {
                    methodStr = limitGroupConfig.getBlackRuleFallback();
                }
                if (StringUtils.isEmpty(methodStr)) {
                    throw new RedisAuxException("no suitable fallback method found in class: " + beanClass.getCanonicalName());
                }
                return LimiterAspect.executeFallBack(group.passArgs(), methodStr, beanClass, method.getParameterTypes(), proceedingJoinPoint.getArgs(), bean);
            }
        return proceedingJoinPoint.proceed();
    }

    private void updateCount(boolean update, boolean success, LimitGroupConfig config) {
        if (update) {
            service.updateCount(success, config);
        }
    }

    private Pair<String, String> getIpAndRequestUrl(LimitGroupConfig limitGroupConfig) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ipAdder = limitGroupConfig.isEnableBlackList() || limitGroupConfig.isEnableWhiteList() ? IpCheckUtil.getIpAddr(request) : "";
            String requestUrl = request.getRequestURI();
            return Pair.of(ipAdder, requestUrl);
        }
        return Pair.of("", "");
    }
}
