package com.xl.redisaux.demo.springmvc.controller;

import com.xl.redisaux.demo.springmvc.service.TestService;
import com.xl.redisaux.limiter.annonations.LimiteExclude;
import com.xl.redisaux.limiter.annonations.LimitGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@LimitGroup(groupId = "1", fallback = "test")
public class GroupLimitDemoController {
    @Resource
    private TestService service;

    @GetMapping("/ok")
    public String ok() {
        testprivate();
        return "ok";
    }

    @GetMapping("/user")
    public String user() {
        return "user";
    }

    @GetMapping("/user/t")
    @LimiteExclude
    public String usert() {
        System.out.println("我被访问了");
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

    @GetMapping("/ok2")
    @LimiteExclude
    public String testprivate() {
        for (int i = 0; i < 6; i++) {
            service.test();
        }
        return "ok2";
    }
}