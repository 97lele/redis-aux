package com.opensource.redisaux.bloomfilter.support;

import com.opensource.redisaux.bloomfilter.core.bitarray.RedisBitArray;
import com.opensource.redisaux.bloomfilter.support.expire.WatiForDeleteKey;
import com.opensource.redisaux.bloomfilter.support.expire.CheckTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 * 新建数组操作类及过期、删除功能
 */
@SuppressWarnings("unchecked")
public class BitArrayOperator {
    private final DefaultRedisScript setBitScript;

    private final DefaultRedisScript getBitScript;

    private final DefaultRedisScript resetBitScript;

    private final RedisTemplate redisTemplate;

    private final CheckTask checkTask;

    private final DefaultRedisScript afterGrowScript;

    public BitArrayOperator(DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript, RedisTemplate redisTemplate, CheckTask checkTask, DefaultRedisScript afterGrowScript) {
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.resetBitScript = resetBitScript;
        this.redisTemplate = redisTemplate;
        this.checkTask = checkTask;
        this.afterGrowScript = afterGrowScript;
    }

    public RedisBitArray createBitArray(String key, boolean enableGrow, double growRate) {
        return new RedisBitArray(this.redisTemplate, key, setBitScript, getBitScript, resetBitScript, afterGrowScript, enableGrow, growRate);
    }

    //过期之后删除
    public void expire(String key, long timeout, TimeUnit timeUnit,boolean local) {
        checkTask.addExpireKey(new WatiForDeleteKey(key, timeUnit.toMillis(timeout), System.currentTimeMillis(),local));
        if(!local){
            redisTemplate.expire(key, timeout, timeUnit);
        }
    }

    public void delete(Collection<String> keys) {
        redisTemplate.delete(keys);
    }

}