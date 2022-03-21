package com.xl.redisaux.limiter.autoconfigure;

import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.limiter.aspect.GroupLimiterAspect;
import com.xl.redisaux.limiter.aspect.NormalLimiterAspect;
import com.xl.redisaux.limiter.component.ActuatorController;
import com.xl.redisaux.limiter.component.DashBoardRequestHandler;
import com.xl.redisaux.limiter.component.LimiterGroupService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;

/**
 * @Author tanjl11
 * @create 2020/7/20 20:19
 * 动态注册bean
 */
@Configuration
public class GroupLimiterRelateBeanRegistry implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        boolean enableConsole = RedisLimiterRegistar.connectConsole.get();

        if (RedisLimiterRegistar.enableGroup.get()) {
            if (!enableConsole) {
                registry(LimiterConstants.ACTUATORCONTROLLER, ActuatorController.class, beanDefinitionRegistry);
            }
            registry(LimiterConstants.GROUP_LIMITER_ASPECT, GroupLimiterAspect.class, beanDefinitionRegistry);
            registry(LimiterConstants.LIMITGROUPSERVICE, LimiterGroupService.class, beanDefinitionRegistry);
        }
        //注册server服务端
        if (enableConsole) {
            registry("dashboardRequestHandler", DashBoardRequestHandler.class, beanDefinitionRegistry);
        }
        //注册普通限流
        registry(LimiterConstants.NORMAL_LIMITER_ASPECT, NormalLimiterAspect.class, beanDefinitionRegistry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    private void registry(String name, Class clazz, BeanDefinitionRegistry beanDefinitionRegistry) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(clazz);
        beanDefinitionRegistry.registerBeanDefinition(name, beanDefinition);

    }
}
