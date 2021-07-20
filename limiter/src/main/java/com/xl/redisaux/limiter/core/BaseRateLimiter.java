package com.xl.redisaux.limiter.core;


import com.xl.redisaux.common.api.LimitGroupConfig;

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
     Map<String, KeyInfoNode> KEY_INFO_NODE_MAP = new ConcurrentHashMap<>();
    /**
     * 存放组的信息
     */
     Map<String, LimitGroupConfig> RATE_LIMIT_GROUP_CONFIG_MAP = new ConcurrentHashMap<>();


    /**
     * 获取
     * @param methodKey
     * @param method
     * @param passArgs
     * @return
     */
    static List<String> getKey(String methodKey, String method, boolean passArgs) {
        KeyInfoNode keyInfoNode;
        if ((keyInfoNode = KEY_INFO_NODE_MAP.get(methodKey)) == null) {
            keyInfoNode = new KeyInfoNode();
            keyInfoNode.fallBackMethod = method;
            keyInfoNode.passArgs = passArgs;
            keyInfoNode.keyNameList = Collections.singletonList(methodKey);
            KEY_INFO_NODE_MAP.put(methodKey, keyInfoNode);
        }
        return keyInfoNode.getKeyNameList();
    }

    /**
     * 配置修改
     * @param limitGroupConfigs
     */
     static void createOrUpdateGroups(List<LimitGroupConfig> limitGroupConfigs) {
        for (LimitGroupConfig group : limitGroupConfigs) {
            RATE_LIMIT_GROUP_CONFIG_MAP.put(group.getId(), group);
        }
    }

    /**
     * 单个修改
     * @param limitGroupConfig
     */
     static void createOrUpdateGroups(LimitGroupConfig limitGroupConfig) {
        RATE_LIMIT_GROUP_CONFIG_MAP.put(limitGroupConfig.getId(),limitGroupConfig);
    }


    /**
     * 限流情况下，是否可以通过执行
     *
     * @param redisLimiter
     * @param methodKey
     * @return
     */
     Boolean canExecute(Annotation redisLimiter, String methodKey);

     Boolean canExecute(LimitGroupConfig limitGroup, String methodKey);


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