package com.xl.redisaux.common.utils.compare;

import java.lang.reflect.Method;

@FunctionalInterface
public interface MethodFilter {
    boolean isSkipCompare(Method method, String methodKey, CompareUtil.CompareContext compareContext);
}