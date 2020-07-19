package com.xl.redisaux.demo.springmvc;

import com.xl.redisaux.limiter.annonations.EnableLimiter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author lulu
 * @Date 2020/7/19 15:58
 */
@SpringBootApplication
@EnableLimiter(enableGroup = true,connectConsole = true)
public class SpringMvcDemoApplication {
    public static void main(String args[]){
        SpringApplication.run(SpringMvcDemoApplication.class, args);
    }
}
