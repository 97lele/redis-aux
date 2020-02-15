package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensource.redisaux.bloomfilter.core.strategy.RedisBloomFilterStrategies;
import com.opensource.redisaux.bloomfilter.core.strategy.Strategy;
import com.opensource.redisaux.bloomfilter.support.RedisBitArrayOperator;
import com.opensource.redisaux.bloomfilter.core.FunnelEnum;
import com.opensource.redisaux.bloomfilter.core.filter.RedisBloomFilter;
import com.opensource.redisaux.bloomfilter.core.filter.RedisBloomFilterItem;
import com.opensource.redisaux.bloomfilter.support.expire.CheckTask;
import com.opensource.redisaux.common.BloomFilterConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 * 布隆过滤器核心配置类
 */
@Configuration
@ConditionalOnClass(RedisBloomFilter.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@SuppressWarnings("unchecked")
public class RedisBloomFilterAutoConfiguration {

    @Autowired
    @Qualifier(BloomFilterConstants.INNERTEMPLATE)
    private RedisTemplate redisTemplate;

    @Bean(name = BloomFilterConstants.INNERTEMPLATE)
    @Primary
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
        template.setEnableTransactionSupport(RedisBloomFilterRegistar.transaction);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(RedisBloomFilter.class)
    public RedisBloomFilter redisBloomFilter() {
        Properties properties = System.getProperties();
        String property = properties.getProperty("sun.arch.data.model");
        Strategy strategy = RedisBloomFilterStrategies.getStrategy(property);
        if (strategy == null) {
            strategy = RedisBloomFilterStrategies.MURMUR128_MITZ_32.getStrategy();
        }
        Map<Class, RedisBloomFilterItem> map = new HashMap(FunnelEnum.values().length);
        for (FunnelEnum funnelEnum : FunnelEnum.values()) {
            RedisBloomFilterItem item = RedisBloomFilterItem.create(funnelEnum.getFunnel(), strategy, redisBitArrayFactory());
            checkTask().addListener(item);
            map.put(funnelEnum.getCode(), item);
        }
        return new RedisBloomFilter(map);
    }

    @Bean(name = "resetBitScript")
    public DefaultRedisScript resetBitScript() {
        DefaultRedisScript<Void> script = new DefaultRedisScript<Void>();
        script.setLocation(new ClassPathResource("ResetBitScript.lua"));
        return script;
    }

    @Bean(name = "setBitScript")
    public DefaultRedisScript setBitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setLocation(new ClassPathResource("SetBitScript.lua"));
        return script;
    }

    @Bean(name = "getBitScript")
    public DefaultRedisScript getBitScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript();
        script.setLocation(new ClassPathResource("GetBitScript.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public DefaultRedisScript afterGrowScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setScriptText("local ttl=redis.call('ttl',KEYS[1]) redis.call('expire',KEYS[2],ttl)");
        return script;
    }

    @Bean
    public RedisBitArrayOperator redisBitArrayFactory() {

        return new RedisBitArrayOperator(
                setBitScript(),
                getBitScript(),
                resetBitScript(),
                redisTemplate,
                checkTask(),
                afterGrowScript()
        );
    }

    @Bean
    public CheckTask checkTask() {
        return new CheckTask();
    }


}