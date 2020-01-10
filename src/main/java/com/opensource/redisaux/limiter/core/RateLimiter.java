package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.RedisAuxException;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:27
 */

public interface RateLimiter {
     int WINDOW_LIMITER =1;
     int TOKEN_LIMITER =2;
     int FUNNEL_LIMITER=3;
     Map<String, List<String>> keyListMap =new ConcurrentHashMap<>();
     Map<String,String> failStrategyExpression=new ConcurrentHashMap<>();
     Map<String,FailStrategy> failStrategyMap=new ConcurrentHashMap<>();
     Map<Class,FailStrategy> failStrategyClassMap=new ConcurrentHashMap<>();

    /**
     * 生成key，并且把对应的失败策略实例化存储
     * @param key
     * @param clazz
     * @return
     */
     static List<String> getKeyAndPutFailStrategyIfAbsent(String key, Class<? extends FailStrategy> clazz,String msg){
         List<String> keyList;
         if((keyList= keyListMap.get(key))==null){
             keyList= Collections.singletonList(key);
             keyListMap.put(key, keyList);
             try {
                 FailStrategy failStrategy;
                 if((failStrategy=failStrategyClassMap.get(clazz))==null){
                     //把实例化的策略存储供下次使用
                     failStrategy=clazz.newInstance();
                     failStrategyClassMap.put(clazz,failStrategy);
                 }
                 failStrategyMap.put(key, failStrategy);
                 failStrategyExpression.put(key,msg);
             } catch (Exception e) {
                 throw new RedisAuxException(String.format("失败策略实例化失败,请检查:%",clazz.getCanonicalName()));
             }
         }
         return keyList;
     }

    /**
     * 限流情况下，是否可以通过执行
     * @param redisLimiter
     * @param key
     * @return
     */
     Boolean canExecute(Annotation redisLimiter, String key);



}