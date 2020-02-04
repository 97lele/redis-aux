package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.opensource.redisaux.bloomfilter.core.*;
import com.opensource.redisaux.bloomfilter.core.filter.RedisBloomFilter;
import com.opensource.redisaux.bloomfilter.core.filter.RedisBloomFilterItem;
import com.opensource.redisaux.bloomfilter.core.strategy.RedisBloomFilterStrategies;
import com.opensource.redisaux.bloomfilter.core.strategy.Strategy;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import com.opensource.redisaux.bloomfilter.support.RedisBitArrayOperator;
import com.opensource.redisaux.bloomfilter.support.expire.CheckTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    @Qualifier(BloomFilterConsts.INNERTEMPLATE)
    private RedisTemplate redisTemplate;


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
            map.put(funnelEnum.getCode(),item);
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
    public DefaultRedisScript afterGrowScript(){
        DefaultRedisScript script=new DefaultRedisScript();
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
    public CheckTask checkTask(){
        return new CheckTask();
    }



}