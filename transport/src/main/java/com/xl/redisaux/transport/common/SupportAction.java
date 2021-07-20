package com.xl.redisaux.transport.common;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 支持的方法
 */
@Getter
@AllArgsConstructor
public enum SupportAction {
    FUNNEL_CHANGE(1, FunnelChangeParam.class, LimitGroupConfig.class),
    WINDOW_CHANGE(2, WindowChangeParam.class, LimitGroupConfig.class),
    TOKEN_CHANGE(3, TokenChangeParam.class, LimitGroupConfig.class),
    CHANGE_IP_RULE(4, ChangeIpRuleParam.class, LimitGroupConfig.class),
    CHANGE_URL_RULE(5, ChangeUrlRuleParam.class, LimitGroupConfig.class),
    CHANGE_LIMIT_MODE(6, ChangeLimitModeParam.class, LimitGroupConfig.class),
    SEND_SERVER_INFO(7, InstanceInfo.class, Void.class),
    GET_RECORD_COUNT(8, String.class, HashMap.class),
    GET_CONFIG_BY_GROUP(9,String.class, LimitGroupConfig.class),
    GET_CONFIGS_BY_GROUPS(10, Set.class,List.class),
    HEART_BEAT(11,Void.class, InstanceInfo.class),
    SUCCESS(0,Object.class,String.class),
    ERROR(-1,Void.class,String.class);
    private int actionCode;
    private Class<?> param;
    private Class<?> result;

    public static Class<?> getActionClass(int actionCode, boolean isResponse) {
        SupportAction supportAction = getAction(actionCode, isResponse);
        return isResponse ? supportAction.result : supportAction.param;
    }

    public static Class<?> getActionClass(RemoteAction action) {
        return getActionClass(action.getActionCode(), action.isResponse);
    }

    public static SupportAction getAction(int actionCode, boolean isResponse) {
        return Arrays.stream(values()).filter(e -> e.getActionCode() == actionCode).findAny().orElseThrow(() -> new RedisAuxException("can't find suitable code"));
    }

    public static SupportAction getAction(RemoteAction action) {
        return getAction(action.getActionCode(), action.isResponse);
    }
}
