package com.xl.redisaux.limiter.core;

import com.xl.redisaux.limiter.annonations.TokenLimiter;
import com.xl.redisaux.limiter.config.LimiteGroupConfig;
import com.xl.redisaux.limiter.config.TokenRateConfig;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:28
 */
@SuppressWarnings("unchecked")
public class TokenRateLimiter implements BaseRateLimiter {

    private RedisTemplate redisTemplate;

    private DefaultRedisScript redisScript;

    public TokenRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript = redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String methodKey) {
        TokenLimiter tokenLimiter = (TokenLimiter) baseLimiter;
        TimeUnit rateUnit = tokenLimiter.tokenRateUnit();
        double capacity = tokenLimiter.capacity();
        double need = tokenLimiter.requestNeed();
        double rate = tokenLimiter.tokenRate();
        String methodName = tokenLimiter.fallback();
        boolean passArgs = tokenLimiter.passArgs();
        List<String> keyList = BaseRateLimiter.getKey(methodKey, methodName, passArgs);
        return handleParam(keyList, capacity, need, rate, rateUnit, tokenLimiter.initToken());

    }

    @Override
    public Boolean canExecute(LimiteGroupConfig limiteGroup, String methodKey) {
        TokenRateConfig tokenRateConfig = limiteGroup.getTokenRateConfig();
        return handleParam(limiteGroup.getTokenKeyName(methodKey), tokenRateConfig.getCapacity(), tokenRateConfig.getRequestNeed(),
                tokenRateConfig.getTokenRate(), tokenRateConfig.getTokenRateUnit(), tokenRateConfig.getInitToken());
    }

    private Boolean handleParam(List<String> keyList, double capacity, double need, double rate, TimeUnit timeUnit, double initToken) {
        long l = timeUnit.toMillis(1);
        double millRate = rate / l;
        long last = System.currentTimeMillis();
        Object[] args = new Double[]{capacity, millRate, need, Double.valueOf(last), initToken};
        Long waitMill  = (Long) redisTemplate.execute(redisScript, keyList, args);
        return Long.valueOf(-1L).equals(waitMill);
    }

}