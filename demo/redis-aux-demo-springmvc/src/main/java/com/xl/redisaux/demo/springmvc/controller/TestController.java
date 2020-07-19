package com.xl.redisaux.demo.springmvc.controller;

import com.xl.redisaux.limiter.annonations.LimiteExclude;
import com.xl.redisaux.limiter.annonations.LimiteGroup;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@LimiteGroup(groupId = "1", fallback = "test")
public class TestController {

    @GetMapping("/ok")
    public String ok() {
        return "ok";
    }

    @GetMapping("/user")
    public String user() {
        return "user";
    }

    @GetMapping("/user/t")
    @LimiteExclude
    public String usert() {
        return "usert";
    }

    public String userBlack() {
        return "非法前缀访问";
    }

    public String ip() {
        return "ip错误";
    }

    public String test() {
        return "too much request";
    }
}