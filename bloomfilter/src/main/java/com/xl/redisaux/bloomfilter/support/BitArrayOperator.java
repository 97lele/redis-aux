package com.xl.redisaux.bloomfilter.support;

import com.xl.redisaux.bloomfilter.core.bitarray.BitArray;
import com.xl.redisaux.bloomfilter.core.bitarray.LocalBitArray;
import com.xl.redisaux.bloomfilter.core.bitarray.RedisBitArray;
import com.xl.redisaux.bloomfilter.support.expire.WatiForDeleteKey;
import com.xl.redisaux.bloomfilter.support.expire.CheckTask;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private final StringRedisTemplate redisTemplate;

    private final CheckTask checkTask;


    public BitArrayOperator(DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript, StringRedisTemplate redisTemplate, CheckTask checkTask) {
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.resetBitScript = resetBitScript;
        this.redisTemplate = redisTemplate;
        this.checkTask = checkTask;
    }

    public BitArray createBitArray(String key, long bitSize, boolean local) {
        if(local){
            return new LocalBitArray(key,bitSize);
        }else{
            return new RedisBitArray(this.redisTemplate, key, setBitScript, getBitScript, resetBitScript,bitSize);
        }
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