package com.xl.redisaux.bloomfilter.core.bitarray;

import com.xl.redisaux.common.consts.BloomFilterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:39
 */
@SuppressWarnings("unchecked")
public class RedisBitArray implements BitArray {


    private RedisTemplate redisTemplate;

    private long bitSize;

    private LinkedList<String> keyList;

    private String key;

    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;

    private DefaultRedisScript resetBitScript;



    public RedisBitArray(RedisTemplate redisTemplate, String key, DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript,long bitSize) {
        if (bitSize > BloomFilterConstants.MAX_REDIS_BIT_SIZE) {
            throw new RedisAuxException("Invalid redis bit size, must small than 2 to the 32");
        }
        this.bitSize = bitSize;
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.keyList = new LinkedList();
        this.keyList.add(key);
        this.resetBitScript = resetBitScript;
    }




    @Override
    public boolean set(long[] index) {
        Object[] value = Arrays.stream(index).boxed().toArray();
        redisTemplate.execute(setBitScript, keyList, value);
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
        return set(res);
    }

    @Override
    public boolean get(long[] index) {
        List<Long> res = getBitScriptExecute(index, index.length);
        boolean exists = res.get(0).equals(BloomFilterConstants.TRUE);
        return exists;
    }

    /**
     * @param index List<long[]>
     * @return
     */
    @Override
    public List<Boolean> getBatch(List index) {
        //index.size*keyList.size
        long[] array = getArrayFromList(index);
        List<Long> list = getBitScriptExecute(array, ((long[]) index.get(0)).length);
        List<Boolean> res = new ArrayList(index.size());
        for (Long temp : list) {
            res.add(Boolean.valueOf(temp.equals(BloomFilterConstants.TRUE)));
        }
        return res;
    }


    @Override
    public long bitSize() {
        return this.bitSize;
    }


    @Override
    public void reset() {
        redisTemplate.execute(resetBitScript, keyList, bitSize);
    }








    /**
     * @param index
     * @return
     */
    private List getBitScriptExecute(long[] index, int size) {
        Object[] value = new Long[index.length + 1];
        value[0] = Long.valueOf(size);
        for (int i = 1; i < value.length; i++) {
            value[i] = Long.valueOf(index[i - 1]);
        }
        List res = (List) redisTemplate.execute(getBitScript, keyList, value);
        return res;
    }

    /**
     * 把list转成long[]
     *
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
            System.arraycopy(temp, 0, res, temp.length * i, temp.length);
        }
        return res;
    }

    public List<String> getKeyList() {
        return keyList;
    }

    @Override
    public void clear() {
        keyList.clear();
        keyList = null;
    }

    @Override
    public String getKey() {
        return this.key;
    }


}
