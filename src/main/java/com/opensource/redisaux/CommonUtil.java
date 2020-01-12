package com.opensource.redisaux;

import com.google.common.base.Joiner;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author: lele
 * @date: 2019/12/25 下午5:17
 */
public class CommonUtil {
    public static String getKeyName(String... key) {
        return Joiner.on(":").appendTo(new StringBuilder(), key).toString();
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
        String key = className + "." + method.getName() + Arrays.toString(method.getGenericParameterTypes());
        return key;
    }


}