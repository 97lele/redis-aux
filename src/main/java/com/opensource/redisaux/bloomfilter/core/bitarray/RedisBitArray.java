package com.opensource.redisaux.bloomfilter.core.bitarray;

import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.support.BloomFilterConsts;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:39
 */
@SuppressWarnings("unchecked")
public class RedisBitArray implements BitArray {


    private RedisTemplate redisTemplate;

    private long bitSize;

    /**
     * 读可以共享，写不可以
     */
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
        this.keyList = new LinkedList();
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
        setBitScriptExecute(index);
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
        setBitScriptExecute(res);
        afterGrow(grow);
        readWriteLock.writeLock().unlock();
        return Boolean.TRUE;
    }

    @Override
    public boolean get(long[] index) {
        readWriteLock.readLock().lock();
        List<Long> res = getBitScriptExecute(index);
        boolean exists = !res.contains(BloomFilterConsts.FALSE);
        readWriteLock.readLock().unlock();
        return exists;
    }

    /**
     *
     * @param index  List<long[]>
     * @return
     */
    @Override
    public List<Boolean> getBatch(List index) {
        //index.size*keyList.size
        readWriteLock.readLock().lock();
        long[] array = getArrayFromList(index);
        List<Long> list = getBitScriptExecute(array);
        List<Boolean> res = new ArrayList(index.size());
        int e = 0;
        for (int q = 0; q < index.size(); q++) {
            boolean hasAdd = false;
            int length =  ((long[]) index.get(q)).length+e;
            for (; e < length; e++) {
                for(int i=0;i<keyList.size();i++){
                    int offset=e+i*array.length;
                    if (list.get(offset).equals(BloomFilterConsts.FALSE)) {
                        res.add(Boolean.FALSE);
                        e = length-1;
                        hasAdd = true;
                        break;
                    }
                }
            }
            if (!hasAdd) {
                res.add(Boolean.TRUE);
            }
        }
        readWriteLock.readLock().unlock();
        return res;
    }


    @Override
    public long bitSize() {
        return this.bitSize;
    }


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
            Long count = (Long) redisTemplate.execute(new RedisCallback() {
                @Override
                public Object doInRedis(RedisConnection redisConnection) throws DataAccessException {
                    return redisConnection.bitCount(keyList.getLast().getBytes());
                }
            });
            if (bitSize * growRate < count) {
                grow = true;
                this.keyList.addLast(this.key + "-" + keyList.size());
            }
        }
        return grow;
    }

    private void afterGrow(boolean grow) {
        List<String> list = new LinkedList();
        list.add(key);
        list.add(keyList.getLast());
        if (grow) {
            redisTemplate.execute(afterGrowScript, list);
        }
    }

    /**
     * @param index
     * @return
     */
    private void setBitScriptExecute(long[] index) {
        Integer length = index.length;
        Object[] value = new Long[length];
        for (int i = 0; i < length; i++) {
            value[i] = Long.valueOf(index[i]);
        }
        LinkedList<String> list = new LinkedList();
        for (String s : keyList) {
            list.add(s);
        }
        list.addFirst(length.toString());
        list.addFirst(list.size() + "");
        redisTemplate.execute(setBitScript, list, value);
    }

    /**
     * @param index
     * @return
     */
    private List<Long> getBitScriptExecute(long[] index) {
        Integer length = index.length;
        Object[] value = new Long[length];
        for (int i = 0; i < length; i++) {
            value[i] = Long.valueOf(index[i]);
        }
        LinkedList<String> list = new LinkedList();
        for (String s : keyList) {
            list.add(s);
        }
        list.addFirst(length.toString());
        list.addFirst(list.size() + "");
        List res = (List) redisTemplate.execute(getBitScript, list, value);
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

    public void clear() {
        keyList.clear();
        keyList = null;
        readWriteLock = null;
    }
}
