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

    /**
     * 缓存，key和具体字节数组
     */
    private final Map<String, BitArray> bitArrayMap;
    /**
     * 缓存，key和对应的hash函数个数
     */
    private final Map<String, Integer> numHashFunctionsMap;

    private final Funnel<? super T> funnel;

    private final Strategy strategy;
    /**
     * 操作类
     * 创建字节数组，过期数组
     */
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
            List<Boolean> list = new ArrayList<>(members.size());
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
     * 这里判断不为空才删除的原因是，有可能里面的键不在里面(有四种类型)
     * 首先清理缓存，然后删掉实际的键
     * @param keys
     */
    public void removeAll(Collection<String> keys) {
        boolean delete = false;
        List<List<String>> list = new ArrayList();
        for (String key : keys) {
            BitArray tBitArray = bitArrayMap.get(key);
            if (tBitArray != null) {
                if(tBitArray instanceof RedisBitArray){
                    list.add(((RedisBitArray) tBitArray).getKeyList());
                }
                bitArrayMap.remove(key);
                numHashFunctionsMap.remove(key);
                delete = true;
                tBitArray.clear();
                tBitArray = null;
            }

        }
        if (delete) {
            List<String> deleteKeys = new LinkedList<String>();
            for (List<String> keyGroup : list) {
                deleteKeys.addAll(keyGroup);
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

    /**
     * 添加内容
     * @param key
     * @param member
     * @param expectedInsertions 预期插入的个数
     * @param fpp 可以接受的错误率
     * @param timeout 键的过期时间
     * @param timeUnit
     * @param local 是否本地数组
     */
    public void put(String key, T member, long expectedInsertions, double fpp, long timeout, TimeUnit timeUnit,boolean local) {
        //参数校验
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        //生成字节缓存并返回是否已经添加
        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp,local);
        //获取字节数组
        BitArray bits = bitArrayMap.get(key);
        //获取hash函数个数
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        //对字节数组下标设置
        strategy.put(member, funnel, numHashFunctions, bits);
        //如果尚未添加过，并且有过期时间的，需要设置
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
        //批量设置的方法
        strategy.putAll(funnel, numHashFunctions, bits, members);
        if (noAdd && timeout != -1) {
            //设置过期时间
            bitArrayOperator.expire(key, timeout, timeUnit,local);
        }
    }

    /**
     * 缓存生成
     * @param bits
     * @param key
     * @param expectedInsertions
     * @param fpp
     * @param local
     * @return
     */
    private Boolean genCache(BitArray bits, String key, long expectedInsertions, double fpp,  boolean local) {
        Boolean noAdd = bits == null;
        //如果尚未生成缓存
        if (noAdd) {
            //计算预估的字节大小
            long numBits = CommonUtil.optimalNumOfBits(expectedInsertions, fpp);
            //创建字节数组
            bits = bitArrayOperator.createBitArray(key, numBits, local);
            //添加缓存
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