package com.xl.redisaux.bloomfilter.support;


import com.xl.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.xl.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.xl.redisaux.bloomfilter.autoconfigure.RedisBloomFilterRegistrar;
import com.xl.redisaux.common.consts.BloomFilterConstants;
import com.xl.redisaux.common.utils.CommonUtil;
import com.xl.redisaux.common.exceptions.RedisAuxException;

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
    /**
     * serializedLambda支持序列化，用于获取lambda表达式的相关属性
     */
    private static Map<Class, SerializedLambda> map = new ConcurrentHashMap();
    private static Map<String, BloomFilterInfo> bloomFilterInfoMap = new ConcurrentHashMap();

    public static <T> BloomFilterInfo resolveFieldName(SFunction<T> sFunction) {
        SerializedLambda lambda = map.get(sFunction.getClass());
        if (lambda == null) {
            try {
                //writeReplace方法用于获取SerializedLamda对象
                Method writeReplace = sFunction.getClass().getDeclaredMethod(BloomFilterConstants.LAMBDA_METHOD_NAME);
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
        //获取属性名
        String fieldName = getFieldName(lambda);
        String capturingClass = lambda.getImplClass();
        //获取键名
        String infoKey = CommonUtil.getKeyName(fieldName, capturingClass);
        //获取包装类信息
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
                if (RedisBloomFilterRegistrar.bloomFilterFieldMap != null) {
                    Map<String, BloomFilterProperty> map = RedisBloomFilterRegistrar.bloomFilterFieldMap.get(annotation.prefix());
                    BloomFilterProperty field = map.get(CommonUtil.getKeyName(annotation.prefix(), fieldName));
                    res = new BloomFilterInfo(annotation.prefix().trim().equals("") ? aClass.getCanonicalName() : annotation.prefix(),
                            field.key().trim().equals("") ? fieldName : field.key(),
                            field.exceptionInsert(),
                            field.fpp(),
                            field.timeout(),
                            field.timeUnit(),

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
        private final boolean local;

        public BloomFilterInfo(String keyPrefix, String keyName, Long exceptionInsert, double fpp, Long timeout, TimeUnit timeUnit, boolean local) {
            this.keyPrefix = keyPrefix;
            this.keyName = keyName;
            this.exceptionInsert = exceptionInsert;
            this.fpp = fpp;
            this.timeout = timeout;
            this.timeUnit = timeUnit;
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