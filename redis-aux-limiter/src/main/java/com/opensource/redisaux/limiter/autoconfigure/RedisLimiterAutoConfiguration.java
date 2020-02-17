package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.FunnelRateLimiter;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.TokenRateLimiter;
import com.opensource.redisaux.limiter.core.WindowRateLimiter;
import com.opensource.redisaux.limiter.core.aspect.RedisLimiterAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: lele
 * @date: 2020/1/2 下午5:12
 */
@SuppressWarnings("unchecked")
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisTemplate.class)
class RedisLimiterAutoConfiguration {

    @Autowired
    @Qualifier(LimiterConstants.LIMITER)
    private RedisTemplate redisTemplate;


    /**
     * 滑动窗口的lua脚本，步骤：
     * 1.记录当前时间戳
     * 2.把小于（当前时间戳-窗口大小得到的时间戳）的key删掉
     * 3.返回该窗口内的成员个数
     *
     * @return
     */
    @Bean
    public DefaultRedisScript windowLimitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setResultType(Boolean.class);
        script.setScriptText("redis.call('zadd',KEYS[1],ARGV[1],ARGV[1]) redis.call('zremrangebyscore',KEYS[1],0,ARGV[2]) return redis.call('zcard',KEYS[1]) <= tonumber(ARGV[3])");
        return script;
    }

    /**
     * 具体思想看lua脚本注释
     *
     * @return
     */
    @Bean
    public DefaultRedisScript tokenLimitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setResultType(Long.class);
        script.setScriptText(tokenRateStr());
        return script;
    }

    /**
     * 具体思想看lua脚本注释
     *
     * @return
     */
    @Bean
    public DefaultRedisScript funnelLimitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setResultType(Boolean.class);
        script.setScriptText(funnelRateStr());
        return script;
    }


    /**
     * 切面
     *
     * @return
     */
    @Bean
    public RedisLimiterAspect limiterAspect() {
        Map<Integer, BaseRateLimiter> map = new HashMap();
        map.put(LimiterConstants.WINDOW_LIMITER, new WindowRateLimiter(redisTemplate, windowLimitScript()));
        map.put(LimiterConstants.TOKEN_LIMITER, new TokenRateLimiter(redisTemplate, tokenLimitScript()));
        map.put(LimiterConstants.FUNNEL_LIMITER, new FunnelRateLimiter(redisTemplate, funnelLimitScript()));
        return new RedisLimiterAspect(map);
    }

    @Bean
    public LimiterGroupService limiterGroupService() {
        return new LimiterGroupService();
    }



    private String funnelRateStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("local limitInfo = redis.call('hmget', KEYS[1], 'capacity', 'funnelRate', 'requestNeed', 'water', 'lastTs')\n")
                .append("local capacity = limitInfo[1]\n").append("local funnelRate = limitInfo[2]\n")
                .append("local requestNeed = limitInfo[3]\n").append("local water = limitInfo[4]\n")
                .append("local lastTs = limitInfo[5]\n").append("if capacity == false then\n")
                .append("    capacity = tonumber(ARGV[1])\n").append("    funnelRate = tonumber(ARGV[2])\n")
                .append("    requestNeed = tonumber(ARGV[3])\n").append("    water = 0\n")
                .append("    lastTs = tonumber(ARGV[4])\n").append("    redis.call('hmset', KEYS[1], 'capacity', capacity, 'funnelRate', funnelRate, 'requestNeed', requestNeed, 'water', water, 'lastTs', lastTs)\n")
                .append("    return true\n").append("else\n").append("    local nowTs = tonumber(ARGV[4])\n")
                .append("    local waterPass = tonumber((nowTs - lastTs) * funnelRate)\n").append("    water = math.max(0, water - waterPass)\n")
                .append("    lastTs = nowTs\n").append("    requestNeed = tonumber(requestNeed)\n").append("    if capacity - water >= requestNeed then\n")
                .append("        water = water + requestNeed\n").append("        redis.call('hmset', KEYS[1], 'water', water, 'lastTs', lastTs)\n")
                .append("        return true\n    end\n    return false\nend");
        return builder.toString();
    }

    private String tokenRateStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("local limitInfo = redis.call('hmget', KEYS[1], 'capacity', 'funnelRate', 'leftToken', 'lastTs')\n")
                .append("local capacity = limitInfo[1]\n").append("local tokenRate = limitInfo[2]\n")
                .append("local leftToken = limitInfo[3]\n").append("local lastTs = limitInfo[4]\n")
                .append("if capacity == false then\n").append("    capacity = tonumber(ARGV[1])\n")
                .append("    tokenRate = tonumber(ARGV[2])\n").append("    leftToken = tonumber(ARGV[5])\n")
                .append("    lastTs = tonumber(ARGV[4])\n").append("    redis.call('hmset', KEYS[1], 'capacity', capacity, 'funnelRate', tokenRate, 'leftToken', leftToken, 'lastTs', lastTs)\n")
                .append("    return -1\nelse\n").append("    local nowTs = tonumber(ARGV[4])\n")
                .append("    local genTokenNum = tonumber((nowTs - lastTs) * tokenRate)\n").append("    leftToken = genTokenNum + leftToken\n")
                .append("    leftToken = math.min(capacity, leftToken)\n    lastTs = nowTs\n    local requestNeed = tonumber(ARGV[3])\n")
                .append("    if leftToken >= requestNeed then\n        leftToken = leftToken - requestNeed\n")
                .append("        redis.call('hmset', KEYS[1], 'leftToken', leftToken, 'lastTs', lastTs)\n")
                .append("        return -1\n    end\n    return (requestNeed - leftToken) / tokenRate\nend");
        return builder.toString();
    }


}