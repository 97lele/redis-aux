package com.xl.redisaux.bloomfilter.autoconfigure;

import com.xl.redisaux.bloomfilter.core.strategy.RedisBloomFilterStrategies;
import com.xl.redisaux.bloomfilter.core.strategy.Strategy;
import com.xl.redisaux.bloomfilter.support.BitArrayOperator;
import com.xl.redisaux.bloomfilter.core.FunnelEnum;
import com.xl.redisaux.bloomfilter.core.filter.RedisBloomFilter;
import com.xl.redisaux.bloomfilter.core.filter.RedisBloomFilterItem;
import com.xl.redisaux.bloomfilter.support.expire.CheckTask;
import com.xl.redisaux.common.consts.BloomFilterConstants;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import javax.annotation.Resource;
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

    @Resource(name = BloomFilterConstants.INNER_TEMPLATE)
    private StringRedisTemplate redisTemplate;


    /**
     * 注册RedisBloomFilter类
     * @return
     */
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
        script.setScriptText(resetBitScriptStr());
        return script;
    }

    @Bean(name = "setBitScript")
    public DefaultRedisScript setBitScript() {
        DefaultRedisScript script = new DefaultRedisScript();
        script.setScriptText(setBitScripStr());
        return script;
    }

    @Bean(name = "getBitScript")
    public DefaultRedisScript getBitScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript();
        script.setScriptText(getBitScriptStr());
        script.setResultType(List.class);
        return script;
    }


    @Bean
    public BitArrayOperator redisBitArrayFactory() {

        return new BitArrayOperator(
                setBitScript(),
                getBitScript(),
                resetBitScript(),
                redisTemplate,
                checkTask()
        );
    }

    @Bean
    public CheckTask checkTask() {
        return new CheckTask();
    }


    private String setBitScripStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("local kL = table.getn(KEYS)\n")
                .append("local aL = table.getn(ARGV)\n")
                .append("for i = 1, kL\n")
                .append("do\n")
                .append("    for k = 1, aL\n")
                .append("    do redis.call('setbit', KEYS[i], tonumber(ARGV[k]), 1)\n")
                .append("    end\nend");
        return builder.toString();
    }

    private String getBitScriptStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("local array = {}\n").append("local aL = table.getn(ARGV) - 1\n")
                .append("local kL = table.getn(KEYS)\n").append("local bitL = ARGV[1]\n")
                .append("local elementSize = aL / bitL\n").append("for index = 1, elementSize\n")
                .append("do local notAdd = true\n").append("    for i = (index - 1) * bitL + 2, index * bitL + 1\n")
                .append("    do\n").append("        local k = 1\n").append("        while (notAdd and k <= kL)\n")
                .append("        do\n").append("            if redis.call('getbit', KEYS[k], ARGV[i]) == 0 then\n")
                .append("                array[index] = 0\n").append("                notAdd = false\n")
                .append("            else\n").append("                k = k + 1\n")
                .append("            end\n").append("        end\n").append("    end\n")
                .append("    if notAdd then array[index] = 1 end\n").append("end\nreturn array");
        return builder.toString();
    }

    private String resetBitScriptStr() {
        StringBuilder builder = new StringBuilder();
        builder.append("local key = KEYS[1]\n")
                .append("local start, last = 0, tonumber(ARGV[1])\n")
                .append("local b = '\\0'\n")
                .append("local mstart, mlast = start % 8, (last + 1) % 8\n")
                .append("if mlast > 0 then\n")
                .append("    local t = math.max(last - mlast + 1, start)\n")
                .append("    for i = t, last do\n")
                .append("        redis.call('SETBIT', key, i, b)\n    end\n    last = t\nend\n")
                .append("if mstart > 0 then\n")
                .append("    local t = math.min(start - mstart + 7, last)\n")
                .append("    for i = start, t do\n")
                .append("        redis.call('SETBIT', key, i, b)\n    end\n    start = t + 1\nend\n")
                .append("local rs, re = start / 8, (last + 1) / 8\nlocal rl = re - rs\nif rl > 0 then\n")
                .append("    redis.call('SETRANGE', key, rs, string.rep(b, rl))\nend\n");
        return builder.toString();
    }


}