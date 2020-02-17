package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.opensource.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.opensource.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.opensource.redisaux.bloomfilter.annonations.EnableBloomFilter;
import com.opensource.redisaux.common.BloomFilterConstants;
import com.opensource.redisaux.common.CommonUtil;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 * 布隆过滤器注册类，主要是lambda和扫描自定义包
 */
@SuppressWarnings("unchecked")
public class RedisBloomFilterRegistar implements ImportBeanDefinitionRegistrar {
    public static Map<String, Map<String, BloomFilterProperty>> bloomFilterFieldMap;
    public static boolean transaction;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableBloomFilter.class.getCanonicalName());
        transaction = (Boolean) attributes.get("transaction");
        String[] scanPaths = (String[]) attributes.get(BloomFilterConstants.SCAPATH);
        if (!scanPaths[0].trim().equals("")) {
            bloomFilterFieldMap = new HashMap();
            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                    false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(BloomFilterPrefix.class));
            for (String basePath : scanPaths) {
                for (BeanDefinition e : scanner.findCandidateComponents(basePath)) {
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(e.getBeanClassName());
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                    if (clazz.isAnnotationPresent(BloomFilterPrefix.class)) {
                        final Map<String, BloomFilterProperty> map = new HashMap();
                        final String prefix = clazz.getAnnotation(BloomFilterPrefix.class).prefix();
                        ReflectionUtils.doWithFields(clazz, new ReflectionUtils.FieldCallback() {
                            @Override
                            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                                field.setAccessible(Boolean.TRUE);
                                if (field.isAnnotationPresent(BloomFilterProperty.class)) {
                                    String key = field.getName();
                                    String keyName = CommonUtil.getKeyName(prefix, key);
                                    map.put(keyName, field.getAnnotation(BloomFilterProperty.class));
                                }
                            }
                        });
                        bloomFilterFieldMap.put(prefix, map);
                    }
                }


            }
            bloomFilterFieldMap = Collections.unmodifiableMap(bloomFilterFieldMap);
        } else {
            System.err.println("=============redisbloomfilter not support work with lambda cause not set the scan path(also require jdk1.8+)=============");
        }

        //指定扫描自己写的符合默认扫描注解的组件
        ClassPathBeanDefinitionScanner scanConfigure =
                new ClassPathBeanDefinitionScanner(registry, true);
        scanConfigure.scan(BloomFilterConstants.PATH);
    }

}