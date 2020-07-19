package com.xl.redisaux.limiter.core;

import com.xl.redisaux.limiter.config.LimiteGroupConfig;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:27
 */
@SuppressWarnings("unchecked")
public interface BaseRateLimiter {

    /**
     * 存放的是keyNameList、是否传参，回调方法名
     */
     Map<String, KeyInfoNode> keyInfoMap = new ConcurrentHashMap();
    /**
     * 存放组的信息
     */
     Map<String, LimiteGroupConfig> rateLimitGroupConfigMap = new ConcurrentHashMap<>();


    /**
     * 生成key，并把该key对应的keynode信息保存
     *
     * @return
     */
    static List<String> getKey(String methodKey, String method, boolean passArgs) {
        KeyInfoNode keyInfoNode;
        if ((keyInfoNode = keyInfoMap.get(methodKey)) == null) {
            keyInfoNode = new KeyInfoNode();
            keyInfoNode.fallBackMethod = method;
            keyInfoNode.passArgs = passArgs;
            keyInfoNode.keyNameList = Collections.singletonList(methodKey);
            keyInfoMap.put(methodKey, keyInfoNode);
        }
        return keyInfoNode.getKeyNameList();
    }

     static void createOrUpdateGroups(List<LimiteGroupConfig> limiteGroup) {
        for (LimiteGroupConfig group : limiteGroup) {
            rateLimitGroupConfigMap.put(group.getId(), group);
        }
    }

     static void createOrUpdateGroups(LimiteGroupConfig limiteGroup) {
        rateLimitGroupConfigMap.put(limiteGroup.getId(),limiteGroup);
    }


    /**
     * 限流情况下，是否可以通过执行
     *
     * @param redisLimiter
     * @param methodKey
     * @return
     */
     Boolean canExecute(Annotation redisLimiter, String methodKey);

     Boolean canExecute(LimiteGroupConfig limiteGroup,String methodKey);


     class KeyInfoNode {
        private List<String> keyNameList;
        private boolean passArgs;
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