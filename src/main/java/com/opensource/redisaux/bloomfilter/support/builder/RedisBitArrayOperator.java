package com.opensource.redisaux.bloomfilter.support.builder;

import com.opensource.redisaux.bloomfilter.core.RedisBitArray;
import com.opensource.redisaux.bloomfilter.core.WatiForDeleteKey;
import com.opensource.redisaux.bloomfilter.support.observer.CheckTask;
import com.opensource.redisaux.bloomfilter.support.observer.KeyExpireListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

public  class RedisBitArrayOperator {
        private final DefaultRedisScript setBitScript;

        private final DefaultRedisScript getBitScript;

        private final DefaultRedisScript resetBitScript;

        private final RedisTemplate redisTemplate;

        private final CheckTask checkTask;


        RedisBitArrayOperator(RedisBitArrayOperatorBuilder builder, CheckTask checkTask) {
            this.redisTemplate = builder.getRedisTemplate();
            this.getBitScript = builder.getGetBitScript();
            this.setBitScript = builder.getSetBitScript();
            this.resetBitScript = builder.getResetBitScript();
            this.checkTask=checkTask;
            //定时清理不用的链接
        }




        public RedisBitArray createBitArray(String key) {
            return new RedisBitArray(this.redisTemplate, key, setBitScript, getBitScript);
        }

        public void reset(List<String> key, Long bitSize) {
            redisTemplate.execute(resetBitScript, key, bitSize);
        }

        public void expire(String key, long timeout, TimeUnit timeUnit){
            checkTask.addExpireKey(new WatiForDeleteKey(key,timeUnit.toMillis(timeout),System.currentTimeMillis()));
            redisTemplate.expire(key,timeout,timeUnit);
        }
        public void delete(Collection<String> keys) {
            redisTemplate.delete(keys);
        }

        public void delete(String key) {
            redisTemplate.delete(key);
        }


}