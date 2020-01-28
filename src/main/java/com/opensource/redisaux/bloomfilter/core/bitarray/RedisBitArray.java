package com.opensource.redisaux.bloomfilter.core.bitarray;

import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:39
 * 操作redis的bitset,lua脚本
 */
public class RedisBitArray implements BitArray {


    private RedisTemplate redisTemplate;

    private long bitSize;

    //读可以共享，写不可以
    private ReadWriteLock readWriteLock;

    private LinkedList<String> keyList;

    private String key;

    private DefaultRedisScript setBitScript;

    private DefaultRedisScript getBitScript;

    private DefaultRedisScript resetBitScript;

    private DefaultRedisScript afterGrowScript;

    private boolean enableGrow;

    private double growRate;

    public RedisBitArray(RedisTemplate redisTemplate, String key, DefaultRedisScript setBitScript, DefaultRedisScript getBitScript, DefaultRedisScript resetBitScript, DefaultRedisScript afterGrowScript, boolean enableGrow, double growRate) {
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.setBitScript = setBitScript;
        this.getBitScript = getBitScript;
        this.keyList = new LinkedList<>();
        this.keyList.add(key);
        this.resetBitScript = resetBitScript;
        this.afterGrowScript = afterGrowScript;
        this.growRate = growRate;
        this.enableGrow = enableGrow;
        readWriteLock = new ReentrantReadWriteLock();
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
        readWriteLock.writeLock().lock();
        boolean grow = ensureCapacity();
        for (String s : keyList) {
            setBitScriptExecute(index, s);
        }
        afterGrow(grow);
        readWriteLock.writeLock().unlock();
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
        readWriteLock.writeLock().lock();
        long[] res = getArrayFromList(index);
        boolean grow = ensureCapacity();
        for (String s : keyList) {
            setBitScriptExecute(res, s);
        }
        afterGrow(grow);
        readWriteLock.writeLock().unlock();
        return Boolean.TRUE;
    }

    @Override
    public boolean get(long[] index) {
        readWriteLock.readLock().lock();
        boolean exists = true;
        for (String s : keyList) {
            List<Long> bits = getBitScriptExecute(index, s);
            if (bits.contains(BloomFilterConsts.FALSE)) {
                exists = false;
            }
        }
        readWriteLock.readLock().unlock();
        return exists;
    }

    @Override
    public List<Boolean> getBatch(List index) {
        //index.size*keyList.size个数
        readWriteLock.readLock().lock();
        List<Boolean> lists = new ArrayList<>(index.size() * keyList.size());
        for (String s : keyList) {
            long[] array = getArrayFromList(index);
            List<Long> list = getBitScriptExecute(array, s);
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
            lists.addAll(res);
        }
        List<Boolean> res = new ArrayList<>(index.size());
        for (int i = 0; i < index.size(); i++) {
            boolean exists = true;
            for (int k = 0; k < keyList.size(); k++) {
                int idx = i + k * index.size();
                if (!lists.get(idx)) {
                    exists = false;
                }
            }
            res.add(exists);
        }
        readWriteLock.readLock().unlock();

        return res;
    }


    @Override
    public long bitSize() {
        return this.bitSize;
    }

    /**
     * 重置改为删除其他多余的，重置第一个
     */
    @Override
    public void reset() {
        readWriteLock.writeLock().lock();
        keyList.clear();
        keyList.add(key);
        readWriteLock.writeLock().unlock();
        redisTemplate.execute(resetBitScript, keyList, bitSize);
    }


    private boolean ensureCapacity() {
        boolean grow = false;
        if (enableGrow) {
            Long count = (Long) redisTemplate.execute((RedisCallback) redisConnection -> redisConnection.bitCount(keyList.getLast().getBytes()));
            if (bitSize * growRate < count) {
                grow = true;
                this.keyList.addLast(this.key + "-" + keyList.size());
            }
        }
        return grow;
    }

    private void afterGrow(boolean grow) {
        if (grow) {
            redisTemplate.execute(afterGrowScript, Arrays.asList(key, keyList.getLast()));
        }
    }

    /**
     * 通过lua脚本设置值
     *
     * @param index
     * @return
     */
    private void setBitScriptExecute(long[] index, String key) {
        Object[] value = new Long[index.length * 2];
        for (int i = 0; i < index.length; i++) {
            value[i * 2] = index[i];
            value[i * 2 + 1] = BloomFilterConsts.TRUE;
        }
        redisTemplate.execute(setBitScript, Arrays.asList(key, index.length + ""), value);
    }

    /**
     * 通过lua脚本返回对应位数的值
     *
     * @param index
     * @return
     */
    private List<Long> getBitScriptExecute(long[] index, String key) {
        Object[] value = Arrays.stream(index).boxed().toArray(Long[]::new);
        List res = (List) redisTemplate.execute(getBitScript, Arrays.asList(key, index.length + ""), value);
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

    public LinkedList<String> getKeyList() {
        return keyList;
    }
}
