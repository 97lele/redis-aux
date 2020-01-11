package com.opensource.redisaux.bloomfilter.support;

import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.opensource.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.opensource.redisaux.bloomfilter.autoconfigure.RedisBloomFilterRegistar;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: lele
 * @date: 2019/12/26 上午8:14
 * 解析lambda，获取字段名
 */
public class GetBloomFilterField {

    private static Map<Class, SerializedLambda> map = new ConcurrentHashMap<>();

    public static <T> BloomFilterInfo resolveFieldName(SFunction<T> sFunction) {
        SerializedLambda lambda = map.get(sFunction.getClass());
        if (lambda == null) {
            try {
                Method writeReplace = sFunction.getClass().getDeclaredMethod(BloomFilterConsts.LAMBDAMETHODNAME);
                writeReplace.setAccessible(Boolean.TRUE);
                lambda = (SerializedLambda) writeReplace.invoke(sFunction);
                map.put(sFunction.getClass(), lambda);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        String fieldName = getFieldName(lambda);
        String capturingClass = lambda.getImplClass().replace("/", ".");
        try {
            Class<?> aClass = Class.forName(capturingClass);
            if (aClass.isAnnotationPresent(BloomFilterPrefix.class)) {
                BloomFilterPrefix annotation = aClass.getAnnotation(BloomFilterPrefix.class);
                if (RedisBloomFilterRegistar.bloomFilterFieldMap != null) {
                    Map<String, BloomFilterProperty> map = RedisBloomFilterRegistar.bloomFilterFieldMap.get(annotation.prefix());
                    BloomFilterProperty field = map.get(CommonUtil.getKeyName(annotation.prefix(), fieldName));
                    return new BloomFilterInfo(annotation.prefix().trim().equals("")?aClass.getCanonicalName():annotation.prefix(),
                            field.key().trim().equals("") ? fieldName : field.key(),
                            field.exceptionInsert(),
                            field.fpp());
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static class BloomFilterInfo {
        private String keyPrefix;
        private String keyName;
        private Long exceptionInsert;
        private Double fpp;

        public BloomFilterInfo(String keyPrefix, String keyName, Long exceptionInsert, Double fpp) {
            this.keyPrefix = keyPrefix;
            this.keyName = keyName;
            this.exceptionInsert = exceptionInsert;
            this.fpp = fpp;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public String getKeyName() {
            return keyName;
        }

        public Long getExceptionInsert() {
            return exceptionInsert;
        }

        public Double getFpp() {
            return fpp;
        }
    }


    private static String getFieldName(SerializedLambda lambda) {
        String getMethodName = lambda.getImplMethodName();
        if (getMethodName.startsWith(BloomFilterConsts.GET)) {
            getMethodName = getMethodName.substring(3);
        } else if (getMethodName.startsWith(BloomFilterConsts.IS)) {
            getMethodName = getMethodName.substring(2);
        } else {
            throw new RedisAuxException("没有相应的属性方法");
        }
        // 小写第一个字母
        return Character.isLowerCase(getMethodName.charAt(0)) ? getMethodName : Character.toLowerCase(getMethodName.charAt(0)) + getMethodName.substring(1);
    }
}