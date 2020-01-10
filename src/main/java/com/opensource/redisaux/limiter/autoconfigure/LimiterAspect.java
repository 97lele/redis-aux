package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.limiter.annonations.LimiterType;
import com.opensource.redisaux.limiter.core.FailStrategy;
import com.opensource.redisaux.limiter.core.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class LimiterAspect {

    @Autowired
    private ExpressionParser expressionParser;

    @Autowired
    DefaultParameterNameDiscoverer defaultParameterNameDiscoverer;

    @Resource(name = "rateLimiterMap")
    private Map<String, RateLimiter> rateLimiterMap;

    private Map<String, Annotation> annotationMap = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.opensource.redisaux.limiter.annonations.TokenLimiter)")
    public void tokenPoint() {

    }

    @Pointcut("@annotation(com.opensource.redisaux.limiter.annonations.WindowLimiter)")
    public void windowPoint() {

    }

    @Pointcut("@annotation(com.opensource.redisaux.limiter.annonations.FunnelLimiter)")
    public void funnelPoint() {

    }

    @Around("funnelPoint()||windowPoint()||tokenPoint()")
    public Object doAroundAdvice(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        //获取所在类名
        String targetName = proceedingJoinPoint.getTarget().getClass().getName();
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
        RateLimiter rateLimiter = rateLimiterMap.get(baseLimiter.mode());
        if (rateLimiter.canExecute(target, methodKey)) {
            return proceedingJoinPoint.proceed();
        } else {
            String msg = RateLimiter.failStrategyExpression.get(methodKey);
            if (msg.contains("#")) {
                String spEL = msg;
                //调用SpelExpressionParser方法解析出注解的实际值
                msg = generateKeyBySpEL(spEL, proceedingJoinPoint);
            }
            //否则执行失败逻辑
            FailStrategy failStrategy = RateLimiter.failStrategyMap.get(methodKey);
            return failStrategy.handle(msg);
        }
    }


    public String generateKeyBySpEL(String spELString, ProceedingJoinPoint joinPoint) {
        Expression expression = expressionParser.parseExpression(spELString);
        //设置上下文对象，需要在里面取
        EvaluationContext context = new StandardEvaluationContext();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = defaultParameterNameDiscoverer.getParameterNames(methodSignature.getMethod());
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }
        return expression.getValue(context).toString();
    }
}