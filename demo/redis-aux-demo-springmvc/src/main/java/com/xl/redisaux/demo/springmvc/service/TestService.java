package com.xl.redisaux.demo.springmvc.service;

import com.xl.redisaux.limiter.annonations.WindowLimiter;
import org.aspectj.weaver.ast.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * <pre>
 *  功能名称
 * </pre>
 *
 * @author tanjl11@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/7/22 22:26
 *  修改内容:
 * </pre>
 */
@Service
public class TestService implements InitializingBean {
    @Value("${mphelper.shard-support:true}")
    private boolean shardSupport;

    @WindowLimiter(passCount = 5,fallback = "no")
    public void test(){
        System.out.println("service-ok");
    }
    public void no(){
        System.out.println("too much");
    }

    @Bean
    public BigDecimal bb(){
        System.out.println(shardSupport);
        return BigDecimal.ONE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(shardSupport);
    }
}
