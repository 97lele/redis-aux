package com.opensource.redisaux.limiter.core;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:27
 */
@SuppressWarnings("unchecked")
public abstract class BaseRateLimiter {
    public final static int WINDOW_LIMITER =1;
    public final static int TOKEN_LIMITER =2;
    public final static int FUNNEL_LIMITER=3;
    //存放的是keyNameList、是否传参，回调方法名
    public static Map<String, KeyInfoNode> keyInfoMap =new ConcurrentHashMap();



    /**
     * 生成key，并且把对应的失败策略实例化存储
     * @return
     */
   static List<String> getKey(String methodKey,String method,boolean passArgs){
        KeyInfoNode keyInfoNode;
        if((keyInfoNode= keyInfoMap.get(methodKey))==null){
            keyInfoNode= new KeyInfoNode();
            keyInfoNode.fallBackMethod=method;
            keyInfoNode.passArgs=passArgs;
            keyInfoNode.keyNameList= Collections.singletonList(methodKey);
            keyInfoMap.put(methodKey, keyInfoNode);
        }
        return keyInfoNode.getKeyNameList();
    }

    /**
     * 限流情况下，是否可以通过执行
     * @param redisLimiter
     * @param methodKey
     * @return
     */
    public   Boolean canExecute(Annotation redisLimiter, String methodKey){return null;};


    public static class KeyInfoNode{

        private  List<String> keyNameList;
        private  boolean passArgs;
        private String fallBackMethod;


        public List<String> getKeyNameList() {
            return keyNameList;
        }

        public boolean isPassArgs() {
            return passArgs;
        }

        public String getFallBackMethod() {
            return fallBackMethod;
        }
    }

}