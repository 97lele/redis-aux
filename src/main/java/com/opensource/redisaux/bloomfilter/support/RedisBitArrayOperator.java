package com.opensource.redisaux.bloomfilter.support;

import com.opensource.redisaux.bloomfilter.core.bitarray.RedisBitArray;
import com.opensource.redisaux.bloomfilter.support.expire.WatiForDeleteKey;
import com.opensource.redisaux.bloomfilter.support.expire.CheckTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class RedisBitArrayOperator {
        private final DefaultRedisScript setBitScript;

        private final DefaultRedisScript getBitScript;

        private final DefaultRedisScript resetBitScript;

        private final RedisTemplate redisTemplate;

        private final CheckTask checkTask;

        private final DefaultRedisScript afterGrowScript;

    public RedisBitArrayOperator(DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript, RedisTemplate redisTemplate, CheckTask checkTask, DefaultRedisScript afterGrowScript) {
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.resetBitScript = resetBitScript;
        this.redisTemplate = redisTemplate;
        this.checkTask = checkTask;
        this.afterGrowScript = afterGrowScript;
    }

    public RedisBitArray createBitArray(String key, boolean enableGrow, double growRate) {
            return new RedisBitArray(this.redisTemplate, key, setBitScript, getBitScript,resetBitScript,afterGrowScript,enableGrow,growRate);
        }

//过期之后删除
        public void expire(String key, long timeout, TimeUnit timeUnit){
            checkTask.addExpireKey(new WatiForDeleteKey(key,timeUnit.toMillis(timeout),System.currentTimeMillis()));
            redisTemplate.expire(key,timeout,timeUnit);
        }
        public void delete(Collection<String> keys) {
            redisTemplate.delete(keys);
        }



}