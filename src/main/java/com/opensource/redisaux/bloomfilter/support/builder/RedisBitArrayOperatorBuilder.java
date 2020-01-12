package com.opensource.redisaux.bloomfilter.support.builder;

import com.opensource.redisaux.bloomfilter.support.observer.CheckTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


/**
 * @author lulu
 * @Date 2020/1/11 15:10
 */
public class RedisBitArrayOperatorBuilder {
    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;

    private RedisTemplate redisTemplate;

    private DefaultRedisScript resetBitScript;



    public DefaultRedisScript getSetBitScript() {
        return setBitScript;
    }

    public DefaultRedisScript getGetBitScript() {
        return getBitScript;
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public DefaultRedisScript getResetBitScript(){return resetBitScript;}


    public RedisBitArrayOperatorBuilder setSetBitScript(DefaultRedisScript setBitScript) {
        this.setBitScript = setBitScript;
        return this;
    }

    public RedisBitArrayOperatorBuilder setResetBitScript(DefaultRedisScript resetBitScript){
        this.resetBitScript=resetBitScript;
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


    public RedisBitArrayOperator build(  CheckTask checkTask
    ) {
        return new RedisBitArrayOperator(this,checkTask);
    }




}
