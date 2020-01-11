package com.opensource.redisaux.bloomfilter.core;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.opensource.redisaux.bloomfilter.support.builder.RedisBitArrayOperator;
import com.opensource.redisaux.bloomfilter.support.observer.CheckTask;
import com.opensource.redisaux.bloomfilter.support.observer.KeyExpireListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.opensource.redisaux.CommonUtil.optimalNumOfBits;
import static com.opensource.redisaux.CommonUtil.optimalNumOfHashFunctions;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:35
 */
public class RedisBloomFilterItem<T> implements KeyExpireListener {


    private final Map<String, RedisBitArray> bitArrayMap;

    private final Map<String, List<String>> keyMap;

    private final Map<String, Integer> numHashFunctionsMap;

    private final Funnel<? super T> funnel;

    private final Strategy strategy;

    private RedisBitArrayOperator redisBitArrayOperator;


    public static <T> RedisBloomFilterItem<T> create(Funnel<? super T> funnel, Strategy strategy
            , RedisBitArrayOperator redisBitArrayOperator) {
        strategy = Optional.ofNullable(strategy).orElse(RedisBloomFilterStrategies.MURMUR128_MITZ_64.getStrategy());
        return new RedisBloomFilterItem<>(funnel, strategy, redisBitArrayOperator);
    }


    private RedisBloomFilterItem(
            Funnel<? super T> funnel,
            Strategy strategy,
            RedisBitArrayOperator redisBitArrayOperator
    ) {
        this.strategy = strategy;
        this.funnel = funnel;
        this.bitArrayMap = new ConcurrentHashMap<>();
        this.numHashFunctionsMap = new ConcurrentHashMap<>();
        this.redisBitArrayOperator = redisBitArrayOperator;
        this.keyMap = new ConcurrentHashMap<>();
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

        if (bits == null) {
            List<Boolean> list = new ArrayList<>(members.size());
            members.forEach(e -> list.add(Boolean.FALSE));
            return list;
        }
        return strategy.mightContains(funnel, numHashFunctions, bits, members);
    }

    public void reset(String key) {
        BitArray<T> tBitArray = bitArrayMap.get(key);
        if (Objects.nonNull(tBitArray)) {
            redisBitArrayOperator.reset(keyMap.get(key), tBitArray.bitSize());
        }
    }

    public void expire(String key, long timeout, TimeUnit timeUnit) {
        if (Objects.nonNull(bitArrayMap.get(key))) {
            if (timeout != -1L) {
                redisBitArrayOperator.expire(key, timeout, timeUnit);
            }
        }
    }

    /**
     * 这里判断不为空才删除的原因是，有可能里面的键不在里面
     *
     * @param iterable
     */
    public void removeAll(Collection<String> iterable) {
        boolean delete = false;
        for (String s : iterable) {
            BitArray<T> tBitArray = bitArrayMap.get(s);
            if (tBitArray != null) {
                tBitArray = null;
                bitArrayMap.remove(s);
                Integer integer = numHashFunctionsMap.get(s);
                integer = null;
                numHashFunctionsMap.remove(s);
                keyMap.remove(s);
                delete = true;
            }

        }
        if (delete) {
            redisBitArrayOperator.delete(iterable);
        }
    }

    public void remove(String key) {
        BitArray<T> tBitArray = bitArrayMap.get(key);
        if (tBitArray != null) {
            tBitArray = null;
            bitArrayMap.remove(key);
            Integer integer = numHashFunctionsMap.get(key);
            integer = null;
            numHashFunctionsMap.remove(key);
            keyMap.remove(key);
            redisBitArrayOperator.delete(key);
        }
    }

    public void put(String key, T member, long expectedInsertions, double fpp, long timeout, TimeUnit timeUnit) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        //获取keyname
        List<String> keyList = keyMap.get(key);
        if (Objects.isNull(keyList)) {
            keyList = Collections.singletonList(key);
            keyMap.put(key, keyList);
        }
        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp);
        RedisBitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.put(member, funnel, numHashFunctions, bits);
        if(noAdd&&timeout!=-1){
            //设置过期时间
            redisBitArrayOperator.expire(key, timeout, timeUnit);
        }
    }

    public void putAll(String key, long expectedInsertions, double fpp, List<T> members, long timeout, TimeUnit timeUnit) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        List<String> keyList = keyMap.get(key);
        if (Objects.isNull(keyList)) {
            keyList = Collections.singletonList(key);
            keyMap.put(key, keyList);
        }
        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp);
        RedisBitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.putAll(funnel, numHashFunctions, bits, members);
        if (noAdd&&timeout!=-1) {
            //设置过期时间
            redisBitArrayOperator.expire(key, timeout, timeUnit);
        }
    }

    private Boolean genCache(RedisBitArray bits,String key,long expectedInsertions,double fpp){
        Boolean noAdd;
        if ((noAdd = Objects.isNull(bits))) {
            long numBits = optimalNumOfBits(expectedInsertions, fpp);

            bits = redisBitArrayOperator.createBitArray(key);
            //获取容量
            bits.setBitSize(numBits);
            bitArrayMap.put(key, bits);
            //获取hash函数数量
            numHashFunctionsMap.put(key, optimalNumOfHashFunctions(expectedInsertions, numBits));
        }
        return noAdd;
    }


    @Override
    public void removeKey(String key) {
        remove(key);
    }
}