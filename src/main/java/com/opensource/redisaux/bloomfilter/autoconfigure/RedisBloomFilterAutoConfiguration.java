package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.opensource.redisaux.bloomfilter.core.*;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import com.opensource.redisaux.bloomfilter.support.builder.RedisBitArrayOperator;
import com.opensource.redisaux.bloomfilter.support.builder.RedisBitArrayOperatorBuilder;
import com.opensource.redisaux.bloomfilter.support.observer.CheckTask;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
@ConditionalOnClass(RedisBloomFilter.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
public class RedisBloomFilterAutoConfiguration {

    @Resource(name = BloomFilterConsts.INNERTEMPLATE)
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
        Map<Class, RedisBloomFilterItem> map = new HashMap<>(FunnelEnum.values().length);
        for (FunnelEnum funnelEnum : FunnelEnum.values()) {
            RedisBloomFilterItem item = RedisBloomFilterItem.create(funnelEnum.getFunnel(), strategy, redisBitArrayFactory());
            checkTask().addListener(item);
            map.put(funnelEnum.getCode(),item);
        }
        return new RedisBloomFilter(map);
    }

    @Bean(name = "resetBitScript")
    public DefaultRedisScript resetBitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setLocation(new ClassPathResource("ResetBitScript.lua"));
        return script;
    }

    @Bean(name = "setBitScript")
    public DefaultRedisScript setBitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setScriptText("for i=1,tonumber(KEYS[2]) do redis.call('setbit',KEYS[1],ARGV[i*2-1],ARGV[i*2]) end");
        return script;
    }

    @Bean(name = "getBitScript")
    public DefaultRedisScript getBitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setScriptText("local array={} for i=1,tonumber(KEYS[2])  do array[i]=redis.call('getbit',KEYS[1],ARGV[i]) end return array");
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisBitArrayOperator redisBitArrayFactory() {
        RedisBitArrayOperatorBuilder builder = new RedisBitArrayOperatorBuilder();
        builder.setGetBitScript(getBitScript())
                .setSetBitScript(setBitScript())
                .setResetBitScript(resetBitScript())
                .setRedisTemplate(redisTemplate);
        return builder.build(checkTask());
    }
    @Bean
    public CheckTask checkTask(){
        return new CheckTask();
    }



}