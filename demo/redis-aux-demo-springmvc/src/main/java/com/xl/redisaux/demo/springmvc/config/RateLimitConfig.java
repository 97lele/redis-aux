package com.xl.redisaux.demo.springmvc.config;

import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.limiter.autoconfigure.LimiterGroupService;
import com.xl.redisaux.limiter.config.FunnelRateConfig;
import com.xl.redisaux.limiter.config.LimiteGroupConfig;
import com.xl.redisaux.limiter.config.TokenRateConfig;
import com.xl.redisaux.limiter.config.WindowRateConfig;
import com.xl.redisaux.limiter.core.handler.GroupHandlerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class RateLimitConfig implements InitializingBean {
    @Autowired
    private LimiterGroupService limiterGroupService;
 
    @Override
    public void afterPropertiesSet() {
        //清除原来的配置
        limiterGroupService.clear("1");
        //新建
        LimiteGroupConfig config = LimiteGroupConfig.of().id("1")
                .remark("this.application").tokenConfig(
                        //令牌桶配置，下面表示令牌桶容量为5，初始桶为3，每1s生产3个令牌，每个请求消耗1个令牌
                        TokenRateConfig.of()
                                .capacity(5.0)
                                .initToken(3.0)
                                .requestNeed(1.0)
                                .tokenRate(3.0)
                                .tokenRateUnit(TimeUnit.SECONDS)
                                .build()
                ).
                        windowConfig(
                                //滑动窗口配置,下面表示10s内只允许5个通过
                                WindowRateConfig.of()
                                        .passCount(5L)
                                        .during(10L)
                                        .duringUnit(TimeUnit.SECONDS)
                                        .build()).currentMode(LimiterConstants.TOKEN_LIMITER)
                //漏斗配置，容纳量为10，每次请求容纳量-1，每3秒增加1个容纳量
                .funnelConfig(FunnelRateConfig.of()
                        .capacity(10.0)
                        .funnelRate(3.0)
                        .funnelRateUnit(TimeUnit.SECONDS)
                        .requestNeed(1.0)
                        .build())
                //黑白名单，网段 xxx.xxx.xxx./24,类似 192.168.0.0-192.168.2.1 以及 192* 分号分隔
                /*.blackRule("127.0.0.1")
                .enableBlackList(true)
                .enableWhiteList(true).
                whiteRule("192.168.0.*")
                */
                .blackRuleFallback("ip")
                //当前限流模式
                .currentMode(LimiterConstants.TOKEN_LIMITER)
                //开启统计，是统计复用该配置下的请求数
                .enableCount(true)
                //统计时间范围,如果没有则从第一次请求开始统计
                .countDuring(1,TimeUnit.MINUTES)
                //url配置,;号分割
                .unableURLPrefix("/user;/qq")
                .enableURLPrefix("/test")
                //url匹配失败后的执行方法
                .urlFallBack("userBlack")
                .build();
        //保存到redis,也可以保存到本地
        limiterGroupService.save(config, true, false);
        //读取redis上的配置
//        limiterGroupService.reload("1");
        //添加对应的拦截器,不然切面中不会执行对应的逻辑，这里也可以实现自己的拦截器并添加上去
        limiterGroupService.addHandler(GroupHandlerFactory.limiteHandler())
                .addHandler(GroupHandlerFactory.ipBlackHandler())
                .addHandler(GroupHandlerFactory.urlPrefixHandler());
        ;
    }
}