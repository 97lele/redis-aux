package com.xl.redisaux.demo.springmvc.controller;

import com.xl.redisaux.common.vo.Result;
import com.xl.redisaux.limiter.annonations.FunnelLimiter;
import com.xl.redisaux.limiter.annonations.TokenLimiter;
import com.xl.redisaux.limiter.annonations.WindowLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class TestController2 {
 
    @GetMapping("ha")
    @WindowLimiter(during = 10,passCount = 5)
    public String test() {
        return "hihi1";
    }
    //每秒通过0.5个请求
    @GetMapping("ha2/{userName}")
    @FunnelLimiter(capacity = 5,funnelRate = 0.5,requestNeed = 1,fallback = "test",passArgs = true)
    public Result<String> test2(@PathVariable("userName")String userName) throws NoSuchMethodException {
        return Result.success("ok");
    }
    //默认为秒，该配置为每秒生成0.5个令牌
    @GetMapping("ha3")
    @TokenLimiter(capacity = 5,tokenRate = 0.5,requestNeed = 1)
    public String test3() {
        return "hihi3";
    }
    //一分钟生产一个令牌
    @GetMapping("ha4")
    @TokenLimiter(capacity = 5,tokenRate = 1,tokenRateUnit = TimeUnit.MINUTES,initToken = 5)
    public String test4() {
        return "hihi4";
    }
 
 
    public Result<String> test(String userName){
        return Result.success("对不起:"+userName+",挤不进去太多人了");
    }
 
 
}