package com.xl.redisaux.bloomfilter.core.filter;

import com.xl.redisaux.bloomfilter.core.bitarray.BitArray;
import com.xl.redisaux.bloomfilter.core.bitarray.RedisBitArray;
import com.xl.redisaux.bloomfilter.core.strategy.Strategy;
import com.xl.redisaux.bloomfilter.support.BitArrayOperator;
import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.xl.redisaux.bloomfilter.support.expire.KeyExpireListener;
import com.xl.redisaux.common.utils.CommonUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


/**
 * @author: lele
 * @date: 2019/12/20 上午11:35
 */
@SuppressWarnings("unchecked")
public class RedisBloomFilterItem<T> implements KeyExpireListener {


    private final Map<String, BitArray> bitArrayMap;


    private final Map<String, Integer> numHashFunctionsMap;

    private final Funnel<? super T> funnel;

    private final Strategy strategy;

    private BitArrayOperator bitArrayOperator;


    public static <T> RedisBloomFilterItem<T> create(Funnel<? super T> funnel, Strategy strategy
            , BitArrayOperator redisBitArrayOperator) {
        return new RedisBloomFilterItem(funnel, strategy, redisBitArrayOperator);
    }


    private RedisBloomFilterItem(
            Funnel<? super T> funnel,
            Strategy strategy,
            BitArrayOperator bitArrayOperator
    ) {
        this.strategy = strategy;
        this.funnel = funnel;
        this.bitArrayMap = new ConcurrentHashMap();
        this.numHashFunctionsMap = new ConcurrentHashMap();
        this.bitArrayOperator = bitArrayOperator;
    }

    public boolean mightContain(String key, T member) {
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        BitArray<T> bits = bitArrayMap.get(key);
        if (bits == null) {
            return false;
        }
        return strategy.mightContain(member, funnel, numHashFunctions, bits);
    }

    public List<Boolean> mightContains(String key, List<T> members) {
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        BitArray<T> bits = bitArrayMap.get(key);
        //如果这个bit不存在，则直接返回false
        if (bits == null) {
            List<Boolean> list = new LinkedList();
            for (int i = 0; i < members.size(); i++) {
                list.add(Boolean.FALSE);
            }
            return list;
        }
        return strategy.mightContains(funnel, numHashFunctions, bits, members);
    }

    public void reset(String key) {
        BitArray tBitArray = bitArrayMap.get(key);
        if (tBitArray != null) {
            tBitArray.reset();
        }
    }

    public void expire(String key, long timeout, TimeUnit timeUnit,boolean local) {
        if (bitArrayMap.get(key) != null && timeout != -1L) {
            bitArrayOperator.expire(key, timeout, timeUnit,local);
        }
    }


    /**
     * 这里判断不为空才删除的原因是，有可能里面的键不在里面
     *
     * @param iterable
     */
    public void removeAll(Collection<String> iterable) {
        boolean delete = false;
        List<List<String>> list = new ArrayList();
        for (String s : iterable) {
            BitArray tBitArray = bitArrayMap.get(s);
            if (tBitArray != null) {
                if(tBitArray instanceof RedisBitArray){
                    list.add(((RedisBitArray) tBitArray).getKeyList());
                }
                bitArrayMap.remove(s);
                numHashFunctionsMap.remove(s);
                delete = true;
                tBitArray.clear();
                tBitArray = null;
            }

        }
        if (delete) {
            List<String> deleteKeys = new LinkedList<String>();
            for (List<String> keyGroup : list) {
                for (String key : keyGroup) {
                    deleteKeys.add(key);
                }
            }
            bitArrayOperator.delete(deleteKeys);
        }
    }

    public void remove(String key) {
        BitArray tBitArray = bitArrayMap.get(key);
        if (tBitArray != null) {
            bitArrayMap.remove(key);
            numHashFunctionsMap.remove(key);
            if(tBitArray instanceof RedisBitArray){
                bitArrayOperator.delete(((RedisBitArray) tBitArray).getKeyList());
            }
            tBitArray.clear();
            tBitArray = null;
        }
    }

    public void put(String key, T member, long expectedInsertions, double fpp, long timeout, TimeUnit timeUnit,boolean local) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        //获取keyname
        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp,local);
        BitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.put(member, funnel, numHashFunctions, bits);
        if (noAdd && timeout != -1) {
            //设置过期时间
            bitArrayOperator.expire(key, timeout, timeUnit,local);
        }
    }

    public void putAll(String key, long expectedInsertions, double fpp, List<T> members, long timeout, TimeUnit timeUnit,boolean local) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        Preconditions.checkArgument(members.size() < expectedInsertions, "once add size (%s) shoud smaller than expectInsertions(%s) ", members.size(), expectedInsertions);

        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp, local);

        BitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.putAll(funnel, numHashFunctions, bits, members);
        if (noAdd && timeout != -1) {
            //设置过期时间
            bitArrayOperator.expire(key, timeout, timeUnit,local);
        }
    }

    private Boolean genCache(BitArray bits, String key, long expectedInsertions, double fpp,  boolean local) {
        Boolean noAdd = bits == null;
        if ((noAdd)) {
            long numBits = CommonUtil.optimalNumOfBits(expectedInsertions, fpp);
            bits = bitArrayOperator.createBitArray(key, numBits, local);
            bitArrayMap.put(key, bits);
            //获取hash函数数量
            numHashFunctionsMap.put(key, CommonUtil.optimalNumOfHashFunctions(expectedInsertions, numBits));
        }
        return noAdd;
    }

    boolean containKey(String key){
        return this.bitArrayMap.get(key)!=null;
    }

    @Override
    public void removeKey(String key) {
        remove(key);
    }

    protected void clear() {
        this.numHashFunctionsMap.clear();
        for (BitArray value : this.bitArrayMap.values()) {
            value.clear();
        }
        this.bitArrayMap.clear();
    }
}