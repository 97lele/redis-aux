package com.opensource.redisaux.limiter.autoconfigure;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.redisaux.common.LimiterConstants;
import com.opensource.redisaux.limiter.core.FunnelRateLimiter;
import com.opensource.redisaux.limiter.core.BaseRateLimiter;
import com.opensource.redisaux.limiter.core.TokenRateLimiter;
import com.opensource.redisaux.limiter.core.WindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
public class RedisLimiterAutoConfiguration {

    @Autowired
    @Qualifier(LimiterConstants.INNERTEMPLATE)
    private RedisTemplate redisTemplate;


    @Bean(name = LimiterConstants.INNERTEMPLATE)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(factory);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.setDefaultTyping(ObjectMapper.DefaultTypeResolverBuilder.noTypeInfoBuilder());
        objectMapper.setDateFormat(dateFormat);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setStringSerializer(stringRedisSerializer);
        template.setDefaultSerializer(jackson2JsonRedisSerializer);
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return template;
    }

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
        script.setLocation(new ClassPathResource("TokenRateLimit.lua"));
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
        script.setLocation(new ClassPathResource("FunnelRateLimit.lua"));
        return script;
    }

    /**
     * 切面
     *
     * @return
     */
    @Bean
    public LimiterAspect limiterAspect() {
        Map<Integer, BaseRateLimiter> map = new HashMap();
        map.put(BaseRateLimiter.WINDOW_LIMITER, new WindowRateLimiter(redisTemplate, windowLimitScript()));
        map.put(BaseRateLimiter.TOKEN_LIMITER, new TokenRateLimiter(redisTemplate, tokenLimitScript()));
        map.put(BaseRateLimiter.FUNNEL_LIMITER, new FunnelRateLimiter(redisTemplate, funnelLimitScript()));
        return new LimiterAspect(map);
    }


}