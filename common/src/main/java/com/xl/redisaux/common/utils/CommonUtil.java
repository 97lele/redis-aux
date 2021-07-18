package com.xl.redisaux.common.utils;



import com.xl.redisaux.common.consts.LimiterConstants;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author: lele
 * @date: 2019/12/25 下午5:17
 */
public class CommonUtil {
    public static String getKeyName(String keyPrefix, String key) {
        return keyPrefix + ":" + key;
    }


    public static int optimalNumOfHashFunctions(long n, long m) {
        // (m / n) * log(2), but avoid truncation due to division!
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    public static long optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    public static String getMethodKey(String className, Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(className).append(".").append(method.getName());
        for (Type genericParameterType : method.getGenericParameterTypes()) {
            builder.append(genericParameterType.getTypeName());
        }
        return builder.toString();
    }


    public static String getLimiterConfigName(String groupId) {
        StringBuilder builder = new StringBuilder(LimiterConstants.LIMITER).append("-").append(groupId);
        return builder.toString();
    }

    public static String getLimiterCountName(String groupId, boolean success) {
        StringBuilder builder = new StringBuilder(LimiterConstants.LIMITER).append("-").append(groupId).append(success ? "-success" : "-fail");
        return builder.toString();
    }

    public static String getLimiterName(String groupId, String methodKey, String type) {
        StringBuilder str = new StringBuilder(LimiterConstants.LIMITER);
        str.append("-").append(groupId).append(":").append(type).append(":").append(methodKey);
        return str.toString();
    }

    public static String getLimiterTypeName(String groupId, String type) {
        StringBuilder str = new StringBuilder(LimiterConstants.LIMITER);
        str.append("-").append(groupId).append(":").append(type).append(":").append("*");
        return str.toString();
    }
}