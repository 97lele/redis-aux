package com.xl.redisaux.transport.common;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 支持的方法
 */
@Getter
@AllArgsConstructor
public enum SupportAction {
    FUNNEL_CHANGE(1, FunnelChangeParam.class, LimiteGroupConfig.class),
    WINDOW_CHANGE(2, WindowChangeParam.class, LimiteGroupConfig.class),
    TOKEN_CHANGE(3, TokenChangeParam.class, LimiteGroupConfig.class),
    CHANGE_IP_RULE(4, ChangeIpRuleParam.class, LimiteGroupConfig.class),
    CHANGE_URL_RULE(5, ChangeUrlRuleParam.class, LimiteGroupConfig.class),
    CHANGE_LIMIT_MODE(6, ChangeLimitModeParam.class, LimiteGroupConfig.class),
    GET_SERVER_INFO(7,Void.class, ServerInfo.class),
    GET_CLIENT_COUNT(8, String.class, HashMap.class);
    private int actionCode;
    private Class<?> param;
    private Class<?> result;

    public static Class<?> getActionClass(int actionCode,boolean isResponse) {
        SupportAction supportAction = Arrays.stream(values()).filter(e -> e.getActionCode() == actionCode).findAny().orElseThrow(() -> new RedisAuxException("can't find suitable code"));
        return isResponse ? supportAction.result : supportAction.param;
    }

    public static Class<?> getActionClass(RemoteAction action) {
        return getActionClass(action.getActionCode(),action.isResponse);
    }
}
