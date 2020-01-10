package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.EnableRedisAux;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * @author: lele
 * @date: 2020/1/2 下午4:10
 */
public class RedisLimiterRegistar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableRedisAux.class.getCanonicalName());
        Boolean enableLimit = (Boolean) attributes.get("enableLimit");
        //如果开启限流，则扫描组件、初始化对应的限流器和切面
        if(enableLimit){
            ClassPathBeanDefinitionScanner scanConfigure =
                    new ClassPathBeanDefinitionScanner(registry, true);
            scanConfigure.scan("com.trendy.util.redis.aux.limiter.autoconfigure");
        }
    }

}