package com.xl.redisaux.bloomfilter.core.bitarray;

import com.xl.redisaux.common.consts.BloomFilterConstants;
import com.xl.redisaux.common.exceptions.RedisAuxException;
import org.springframework.data.redis.core.StringRedisTemplate;
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


    private StringRedisTemplate redisTemplate;

    private long bitSize;

    private ArrayList<String> keyList;

    private String key;

    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;

    private DefaultRedisScript resetBitScript;


    public RedisBitArray(StringRedisTemplate redisTemplate, String key, DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript, long bitSize) {
        if (bitSize > BloomFilterConstants.MAX_REDIS_BIT_SIZE) {
            throw new RedisAuxException("Invalid redis bit size, must small than 2 to the 32");
        }
        this.bitSize = bitSize;
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.keyList = new ArrayList<>(2);
        this.keyList.add(key);
        this.resetBitScript = resetBitScript;
    }

    /**
     * 设置bit
     * @param index
     * @return
     */
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

    /**
     * 查询各下标位置
     * @param index
     * @return
     */
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
        //index.size*keyList.size 转成一维数组
        long[] array = getArrayFromList(index);
        //((long[]) index.get(0)).length 代表一个key对应的下标个数
        List<Long> list = getBitScriptExecute(array, ((long[]) index.get(0)).length);
        List<Boolean> res = new ArrayList(index.size());
        for (Long temp : list) {
            res.add(temp.equals(BloomFilterConstants.TRUE));
        }
        return res;
    }


    @Override
    public long bitSize() {
        return this.bitSize;
    }


    @Override
    public void reset() {
        //这个脚本大概率执行不了,改为删除
       //redisTemplate.execute(resetBitScript, keyList, bitSize);
        redisTemplate.delete(key);
    }


    /**
     * @param index
     * @return
     */
    private List getBitScriptExecute(long[] index, int size) {
        Object[] value = new Long[index.length + 1];
        value[0] = (long) size;
        for (int i = 1; i < value.length; i++) {
            value[i] = index[i - 1];
        }
        //value 第一位是多少个下标为一组，其余都是要确认的下标
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
        redisTemplate.delete(key);
        keyList = null;
    }

    @Override
    public String getKey() {
        return this.key;
    }


}
