package com.opensource.redisaux.common;

import io.lettuce.core.RedisConnectionException;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    public static <T> T execute(Supplier<T> function, RedisTemplate redisTemplate) {
        try {
            return function.get();
        } catch (RedisConnectionException exception) {
            RedisConnectionUtils.unbindConnection(redisTemplate.getConnectionFactory());
        }
        return null;
    }

    public static String getLimiterConfigName(String groupId) {
        StringBuilder builder = new StringBuilder(LimiterConstants.LIMITER).append("-").append(groupId);
        return builder.toString();
    }

    public static String getLimiterCountName(String groupId, boolean success) {
        StringBuilder builder = new StringBuilder(LimiterConstants.LIMITER).append("-").append(groupId).append(success ? "-success" : "-fail");
        return builder.toString();
    }

    public static String getHighQPS(long total, TimeUnit duringTimeUnit, long during) {
        if (during == -1) {
            return 0+"";
        }
        long l = duringTimeUnit.toDays(during);
        if(l==0){
            l=1;
        }
        BigDecimal total1 = new BigDecimal(total).multiply(BigDecimal.valueOf(0.8));
        BigDecimal t=new BigDecimal(l).multiply(BigDecimal.valueOf(0.2)).multiply(BigDecimal.valueOf(86400));
        return total1.divide(t,3,BigDecimal.ROUND_UP).toString();
    }

    public static String getQPS(long total, TimeUnit duringTimeUnit, long during) {
        if (during == -1) {
            return 0+"";
        }
        long second = duringTimeUnit.toSeconds(during);
        BigDecimal bigDecimal=new BigDecimal(total);
        BigDecimal bigDecimal1=new BigDecimal(second);

        return bigDecimal.divide(bigDecimal1,3, BigDecimal.ROUND_UP).toString();
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