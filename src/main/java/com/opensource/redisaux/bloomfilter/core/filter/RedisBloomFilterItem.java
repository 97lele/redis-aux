package com.opensource.redisaux.bloomfilter.core.filter;

import com.google.common.base.Preconditions;
import com.google.common.hash.Funnel;
import com.opensource.redisaux.bloomfilter.core.bitarray.BitArray;
import com.opensource.redisaux.bloomfilter.core.bitarray.RedisBitArray;
import com.opensource.redisaux.bloomfilter.core.strategy.Strategy;
import com.opensource.redisaux.bloomfilter.support.RedisBitArrayOperator;
import com.opensource.redisaux.bloomfilter.support.expire.KeyExpireListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import static com.opensource.redisaux.CommonUtil.optimalNumOfBits;
import static com.opensource.redisaux.CommonUtil.optimalNumOfHashFunctions;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:35
 */
public class RedisBloomFilterItem<T> implements KeyExpireListener {


    private final Map<String, RedisBitArray> bitArrayMap;

    private final Map<String, Integer> numHashFunctionsMap;

    private final Funnel<? super T> funnel;

    private final Strategy strategy;

    private RedisBitArrayOperator redisBitArrayOperator;


    public static <T> RedisBloomFilterItem<T> create(Funnel<? super T> funnel, Strategy strategy
            , RedisBitArrayOperator redisBitArrayOperator) {
        return new RedisBloomFilterItem(funnel, strategy, redisBitArrayOperator);
    }


    private RedisBloomFilterItem(
            Funnel<? super T> funnel,
            Strategy strategy,
            RedisBitArrayOperator redisBitArrayOperator
    ) {
        this.strategy = strategy;
        this.funnel = funnel;
        this.bitArrayMap = new ConcurrentHashMap();
        this.numHashFunctionsMap = new ConcurrentHashMap();
        this.redisBitArrayOperator = redisBitArrayOperator;
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
        RedisBitArray tBitArray = bitArrayMap.get(key);
        if (tBitArray!=null) {
           tBitArray.reset();
        }
    }

    public void expire(String key, long timeout, TimeUnit timeUnit) {
        if (bitArrayMap.get(key)!=null&&timeout!=-1L) {
                redisBitArrayOperator.expire(key, timeout, timeUnit);
        }
    }

    public int getElementSize(String key){
        if(bitArrayMap.get(key)!=null){
            return bitArrayMap.get(key).getSize();
        }
        return -1;
    }

    /**
     * 这里判断不为空才删除的原因是，有可能里面的键不在里面
     *
     * @param iterable
     */
    public void removeAll(Collection<String> iterable) {
        boolean delete = false;
        List<List<String>> list=new ArrayList();
        for (String s : iterable) {
            RedisBitArray tBitArray = bitArrayMap.get(s);
            if (tBitArray != null) {
                list.add(tBitArray.getKeyList());
                bitArrayMap.remove(s);
                numHashFunctionsMap.remove(s);
                delete = true;
            }

        }
        if (delete) {
            List<String> deleteKeys =new LinkedList<String>();
            for (List<String> keyGroup : list) {
                for (String key : keyGroup) {
                    deleteKeys.add(key);
                }
            }
            redisBitArrayOperator.delete(deleteKeys);
        }
    }

    public void remove(String key) {
        RedisBitArray tBitArray = bitArrayMap.get(key);
        if (tBitArray != null) {
            bitArrayMap.remove(key);
            numHashFunctionsMap.remove(key);
            redisBitArrayOperator.delete(tBitArray.getKeyList());
            tBitArray.clear();
            tBitArray=null;
        }
    }

    public void put(String key, T member, long expectedInsertions, double fpp, long timeout, TimeUnit timeUnit,boolean enableGrow,double growRate) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        //获取keyname
        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp,enableGrow,growRate);
        RedisBitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.put(member, funnel, numHashFunctions, bits);
        if(noAdd&&timeout!=-1){
            //设置过期时间
            redisBitArrayOperator.expire(key, timeout, timeUnit);
        }
    }

    public void putAll(String key, long expectedInsertions, double fpp, List<T> members, long timeout, TimeUnit timeUnit,boolean enableGrow,double growRate) {
        Preconditions.checkArgument(
                expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
        Preconditions.checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
        Preconditions.checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
        Preconditions.checkArgument(members.size()<expectedInsertions,"once add size (%s) shoud smaller than expectInsertions(%s) ",members.size(),expectedInsertions);

        Boolean noAdd = genCache(bitArrayMap.get(key), key, expectedInsertions, fpp,enableGrow,growRate);

        RedisBitArray bits = bitArrayMap.get(key);
        Integer numHashFunctions = numHashFunctionsMap.get(key);
        strategy.putAll(funnel, numHashFunctions, bits, members);
        if (noAdd&&timeout!=-1) {
            //设置过期时间
            redisBitArrayOperator.expire(key, timeout, timeUnit);
        }
    }

    private Boolean genCache(RedisBitArray bits,String key,long expectedInsertions,double fpp,boolean enableGrow,double growRate){
        Boolean noAdd = bits==null;
        if ((noAdd)) {
            long numBits = optimalNumOfBits(expectedInsertions, fpp);
            bits = redisBitArrayOperator.createBitArray(key,enableGrow,growRate);
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

    protected void clear(){
        this.numHashFunctionsMap.clear();
        for (RedisBitArray value : this.bitArrayMap.values()) {
            value.clear();
        }
        this.bitArrayMap.clear();
    }
}