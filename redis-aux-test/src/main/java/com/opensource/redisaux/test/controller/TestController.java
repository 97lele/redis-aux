package com.opensource.redisaux.test.controller;

import com.opensource.redisaux.limiter.annonations.FunnelLimiter;
import com.opensource.redisaux.limiter.annonations.TokenLimiter;
import com.opensource.redisaux.limiter.annonations.WindowLimiter;
import com.opensource.redisaux.test.entity.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("ha")
    @WindowLimiter(during = 10, value = 5)
    public String test() {
        return "hihi1";
    }

    @GetMapping("ha2/{userName}")
    @FunnelLimiter(capacity = 5, passRate = 0.5, addWater = 1, fallback = "test", passArgs = true)
    public Result<String> test2(@PathVariable("userName") String userName) throws NoSuchMethodException {

        return Result.success("ok");
    }

    @GetMapping("ha3")
    @TokenLimiter(capacity = 5, rate = 0.5, need = 1)
    public String test3() {
        return "hihi3";
    }

    @GetMapping("ha4")
    @TokenLimiter(capacity = 1, rate = 0.5, need = 1, isAbort = true, timeout = 2000)
    public String test4() {
        return "hihi4";
    }

    @GetMapping("ha5")
    @TokenLimiter(capacity = 1, rate = 0.5, need = 1, isAbort = true)
    public String test5() {
        return "hihi5";
    }

    public Result<String> test(String userName) {
        return Result.success("对不起:" + userName + ",挤不进去太多人了");
    }

}