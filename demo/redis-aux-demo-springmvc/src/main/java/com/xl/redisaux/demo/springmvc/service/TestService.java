package com.xl.redisaux.demo.springmvc.service;

import com.xl.redisaux.limiter.annonations.WindowLimiter;
import org.springframework.stereotype.Service;

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
public class TestService {
    @WindowLimiter(passCount = 5,fallback = "no")
    public void test(){
        System.out.println("service-ok");
    }
    public void no(){
        System.out.println("too much");
    }
}
