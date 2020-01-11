package com.opensource.redisaux.bloomfilter.core;

import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:39
 * 操作redis的bitset,lua脚本
 */
 class RedisBitArray implements BitArray {


    private RedisTemplate redisTemplate;

    private long bitSize;

    private String key;

    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;


    public RedisBitArray(RedisTemplate redisTemplate, String key, DefaultRedisScript setBitScript, DefaultRedisScript getBitScript) {
        this.redisTemplate = redisTemplate;
        this.key=key;
        this.setBitScript=setBitScript;
        this.getBitScript=getBitScript;

    }


    @Override
    public void setBitSize(long bitSize) {
        if (bitSize > BloomFilterConsts.MAX_REDIS_BIT_SIZE) {
            throw new RedisAuxException("Invalid redis bit size, must small than 2 to the 32");
        }
        this.bitSize = bitSize;
    }

    @Override
    public boolean set(long[] index) {
        setBitScriptExecute(index);
        return Boolean.TRUE;
    }

    /**
     * 通过lua脚本设置
     *
     * @param index
     * @return
     */
    @Override
    public boolean setBatch(List index) {
        long[] res = getArrayFromList(index);
        setBitScriptExecute(res);
        return Boolean.TRUE;
    }

    @Override
    public boolean get(long[] index) {
        List<Long> bits = getBitScriptExecute(index);
        return !bits.contains(BloomFilterConsts.FALSE);
    }

    @Override
    public List<Boolean> getBatch(List index) {

        long[] array = getArrayFromList(index);
        List<Long> list = getBitScriptExecute(array);
        List<Boolean> res = new ArrayList<>(index.size());
        int e = 0;
        //根据键所对应的区间查找是否有false
        for (int q = 0; q < index.size(); q++) {
            Boolean hasAdd = Boolean.FALSE;
            int length = ((long[]) index.get(q)).length + e;
            for (; e < length; e++) {
                if (list.get(e).equals(BloomFilterConsts.FALSE)) {
                    res.add(Boolean.FALSE);
                    e = length;
                    hasAdd = Boolean.TRUE;
                    break;
                }
            }
            if (!hasAdd) {
                res.add(Boolean.TRUE);
            }
        }
        return res;
    }


    @Override
    public long bitSize() {
        return this.bitSize;
    }

    /**
     * 通过lua脚本设置值
     * @param index
     * @return
     */
    private void setBitScriptExecute(long[] index) {
        Object[] value = new Long[index.length * 2];
        for (int i = 0; i < index.length; i++) {
            value[i * 2] = index[i];
            value[i * 2 + 1] = BloomFilterConsts.TRUE;
        }
        redisTemplate.execute(setBitScript, Arrays.asList(key,index.length+""), value);
    }

    /**
     * 通过lua脚本返回对应位数的值
     * @param index
     * @return
     */
    private List<Long> getBitScriptExecute(long[] index) {
        Object[] value = Arrays.stream(index).boxed().toArray(Long[]::new);
        List res = (List) redisTemplate.execute(getBitScript, Arrays.asList(key,index.length+""), value);
        return res;
    }

    /**
     * 把list转成long[]
     * @param index
     * @return
     */
    private long[] getArrayFromList(List index) {
        int length = 0;
        for (Object o : index) {
            long[] temp = (long[]) o;
            length += temp.length;
        }
        long[] res = new long[length];
        for (int i = 0; i < index.size(); i++) {
            long[] temp = (long[]) index.get(i);
            System.arraycopy(temp, 0, res, temp.length*i, temp.length);
        }
        return res;
    }

}
