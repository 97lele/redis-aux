package com.opensource.redisaux.bloomfilter.support;


import com.opensource.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.opensource.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilterRegistar;
import com.opensource.redisaux.common.BloomFilterConstants;
import com.opensource.redisaux.common.CommonUtil;
import com.opensource.redisaux.common.RedisAuxException;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2019/12/26 上午8:14
 * 解析lambda，获取字段名
 * 1.8才支持
 */
@SuppressWarnings("unchecked")
public class GetBloomFilterField {

    private static Map<Class, SerializedLambda> map = new ConcurrentHashMap();
    private static Map<String, BloomFilterInfo> bloomFilterInfoMap = new ConcurrentHashMap();

    public static <T> BloomFilterInfo resolveFieldName(SFunction<T> sFunction) {
        SerializedLambda lambda = map.get(sFunction.getClass());
        if (lambda == null) {
            try {
                Method writeReplace = sFunction.getClass().getDeclaredMethod(BloomFilterConstants.LAMBDAMETHODNAME);
                writeReplace.setAccessible(true);
                lambda = (SerializedLambda) writeReplace.invoke(sFunction);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            map.put(sFunction.getClass(), lambda);
        }
        String fieldName = getFieldName(lambda);
        String capturingClass = lambda.getImplClass();
        String infoKey = CommonUtil.getKeyName(fieldName, capturingClass);
        BloomFilterInfo res = bloomFilterInfoMap.get(infoKey);
        if (res == null) {
            capturingClass = capturingClass.replace("/", ".");
            Class<?> aClass = null;
            try {
                aClass = Class.forName(capturingClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (aClass.isAnnotationPresent(BloomFilterPrefix.class)) {
                BloomFilterPrefix annotation = aClass.getAnnotation(BloomFilterPrefix.class);
                if (RedisBloomFilterRegistar.bloomFilterFieldMap != null) {
                    Map<String, BloomFilterProperty> map = RedisBloomFilterRegistar.bloomFilterFieldMap.get(annotation.prefix());
                    BloomFilterProperty field = map.get(CommonUtil.getKeyName(annotation.prefix(), fieldName));
                    res = new BloomFilterInfo(annotation.prefix().trim().equals("") ? aClass.getCanonicalName() : annotation.prefix(),
                            field.key().trim().equals("") ? fieldName : field.key(),
                            field.exceptionInsert(),
                            field.fpp(),
                            field.timeout(),
                            field.timeUnit(),
                            field.enableGrow(),
                            field.growRate(),
                    field.local()
                    );
                }
            }
            if (res != null) {
                bloomFilterInfoMap.put(infoKey, res);
            }

        }
        return res;

    }

    public static class BloomFilterInfo {
        private final String keyPrefix;
        private final String keyName;
        private final long exceptionInsert;
        private final double fpp;
        private final long timeout;
        private final TimeUnit timeUnit;
        private final boolean enableGrow;
        private final double growRate;
        private final boolean local;

        public BloomFilterInfo(String keyPrefix, String keyName, Long exceptionInsert, double fpp, Long timeout, TimeUnit timeUnit, boolean enableGrow, double growRate,boolean local) {
            this.keyPrefix = keyPrefix;
            this.keyName = keyName;
            this.exceptionInsert = exceptionInsert;
            this.fpp = fpp;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
            this.enableGrow = enableGrow;
            this.growRate = growRate;
            this.local=local;

        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public String getKeyName() {
            return keyName;
        }

        public long getExceptionInsert() {
            return exceptionInsert;
        }

        public double getFpp() {
            return fpp;
        }

        public long getTimeout() {
            return timeout;
        }

        public TimeUnit getTimeUnit() {
            return timeUnit;
        }

        public boolean isEnableGrow() {
            return enableGrow;
        }

        public double getGrowRate() {
            return growRate;
        }

        public boolean isLocal(){return local;}
    }


    private static String getFieldName(SerializedLambda lambda) {
        String getMethodName = lambda.getImplMethodName();
        if (getMethodName.startsWith(BloomFilterConstants.GET)) {
            getMethodName = getMethodName.substring(3);
        } else if (getMethodName.startsWith(BloomFilterConstants.IS)) {
            getMethodName = getMethodName.substring(2);
        } else {
            throw new RedisAuxException("没有相应的属性方法");
        }
        // 小写第一个字母
        return Character.isLowerCase(getMethodName.charAt(0)) ? getMethodName : Character.toLowerCase(getMethodName.charAt(0)) + getMethodName.substring(1);
    }
}