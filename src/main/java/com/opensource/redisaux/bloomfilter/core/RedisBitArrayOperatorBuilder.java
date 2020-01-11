package com.opensource.redisaux.bloomfilter.core;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.List;

/**
 * @author lulu
 * @Date 2020/1/11 15:10
 */
public class RedisBitArrayOperatorBuilder {
    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;

    private RedisTemplate redisTemplate;

    private DefaultRedisScript resetBitScript;

    public RedisBitArrayOperatorBuilder setSetBitScript(DefaultRedisScript setBitScript) {
        this.setBitScript = setBitScript;
        return this;
    }

    public RedisBitArrayOperatorBuilder setGetBitScript(DefaultRedisScript getBitScript) {
        this.getBitScript = getBitScript;
        return this;
    }

    public RedisBitArrayOperatorBuilder setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        return this;
    }

    public RedisBitArrayOperatorBuilder setResetBitScript(DefaultRedisScript resetBitScript) {
        this.resetBitScript = resetBitScript;
        return this;
    }


    public RedisBitArrayOperator build() {
        return new RedisBitArrayOperator(this);
    }

    /**
     * 负责创建RedisBitArray对象
     */
    public static class RedisBitArrayOperator {
        private final DefaultRedisScript setBitScript;

        private final DefaultRedisScript getBitScript;

        private final DefaultRedisScript resetBitScript;

        private final RedisTemplate redisTemplate;

        RedisBitArrayOperator(RedisBitArrayOperatorBuilder builder) {
            this.redisTemplate = builder.redisTemplate;
            this.getBitScript = builder.getBitScript;
            this.setBitScript = builder.setBitScript;
            this.resetBitScript = builder.resetBitScript;
        }

        public RedisBitArray createBitArray(String key) {
            return new RedisBitArray(this.redisTemplate, key, setBitScript, getBitScript);
        }

        public void reset(List<String> key, Long bitSize) {
            redisTemplate.execute(resetBitScript, key, bitSize);
        }

        public void delete(Collection<String> keys) {
            redisTemplate.delete(keys);
        }

        public void delete(String key) {
            redisTemplate.delete(key);
        }
    }

}
