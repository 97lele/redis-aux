package com.xl.redisaux.bloomfilter.autoconfigure;


import com.xl.redisaux.common.consts.BloomFilterConstants;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author lulu
 * @Date 2020/2/16 16:47
 */
@Configuration
@ConditionalOnClass(RedisConnectionFactory.class)
public class BloomRedisTemplateConfig {
    @Bean(name = BloomFilterConstants.INNER_TEMPLATE)
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setEnableTransactionSupport(RedisBloomFilterRegistrar.transaction);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
