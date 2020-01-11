package com.opensource.redisaux.bloomfilter.core;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;

/**
 * @author lulu
 * @Date 2020/1/11 15:10
 */
public class RedisBitArrayFactoryBuilder {
    private  DefaultRedisScript setBitScript;

    private  DefaultRedisScript getBitScript;

    private  RedisTemplate redisTemplate;

    public RedisBitArrayFactoryBuilder setSetBitScript(DefaultRedisScript setBitScript){
        this.setBitScript=setBitScript;
        return this;
    }
    public RedisBitArrayFactoryBuilder setGetBitScript(DefaultRedisScript getBitScript){
        this.getBitScript=getBitScript;
        return this;
    }
    public RedisBitArrayFactoryBuilder setRedisTemplate(RedisTemplate redisTemplate){
        this.redisTemplate=redisTemplate;
        return this;
    }

    public DefaultRedisScript getSetBitScript() {
        return setBitScript;
    }

    public DefaultRedisScript getGetBitScript() {
        return getBitScript;
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public RedisBitArrayFactory build(){
        return new RedisBitArrayFactory(this);
    }
    /**
     * 负责创建RedisBitArray对象
     */
    public static class RedisBitArrayFactory{
        private final DefaultRedisScript setBitScript;

        private final DefaultRedisScript getBitScript;

        private final RedisTemplate redisTemplate;
        RedisBitArrayFactory(RedisBitArrayFactoryBuilder builder){
            this.redisTemplate=builder.getRedisTemplate();
            this.getBitScript=builder.getGetBitScript();
            this.setBitScript=builder.getSetBitScript();
        }

        public  RedisBitArray of(String key){
            return new RedisBitArray(this.redisTemplate,key,setBitScript,getBitScript);
        }

        public void delete(Collection<String> keys){
            redisTemplate.delete(keys);
        }
        public void delete(String key){
            redisTemplate.delete(key);
        }
    }

}
