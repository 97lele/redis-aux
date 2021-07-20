package com.xl.redisaux.bloomfilter.autoconfigure;

import com.xl.redisaux.bloomfilter.annonations.BloomFilterPrefix;
import com.xl.redisaux.bloomfilter.annonations.BloomFilterProperty;
import com.xl.redisaux.bloomfilter.annonations.EnableBloomFilter;
import com.xl.redisaux.common.consts.BloomFilterConstants;
import com.xl.redisaux.common.utils.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 * 布隆过滤器注册类，主要是lambda和扫描自定义包
 */
@SuppressWarnings("unchecked")
@Slf4j
public class RedisBloomFilterRegistrar implements ImportBeanDefinitionRegistrar {
    public static Map<String, Map<String, BloomFilterProperty>> bloomFilterFieldMap;
    public static boolean transaction;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableBloomFilter.class.getCanonicalName());
        transaction = (Boolean) attributes.get("transaction");
        String[] scanPaths = (String[]) attributes.get(BloomFilterConstants.SCAPATH);
        //扫描并存储注解上的信息
        if (!scanPaths[0].isEmpty()) {
            bloomFilterFieldMap = new HashMap<>();
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
                        //查看clazz是否有对应的注解，并生成键名和对应的注解
                        ReflectionUtils.doWithFields(clazz, field -> {
                            field.setAccessible(Boolean.TRUE);
                            if (field.isAnnotationPresent(BloomFilterProperty.class)) {
                                String key = field.getName();
                                String keyName = CommonUtil.getKeyName(prefix, key);
                                map.put(keyName, field.getAnnotation(BloomFilterProperty.class));
                            }
                        });
                        bloomFilterFieldMap.put(prefix, map);
                    }
                }
            }
            bloomFilterFieldMap = Collections.unmodifiableMap(bloomFilterFieldMap);
        } else {
            log.warn("=============redisbloomfilter not support work with lambda cause not set the scan path(also require jdk1.8+)=============");
        }

        //指定扫描自己写的符合默认扫描注解的组件
        ClassPathBeanDefinitionScanner scanConfigure =
                new ClassPathBeanDefinitionScanner(registry, true);
        scanConfigure.scan(BloomFilterConstants.PATH);
    }

}