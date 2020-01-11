package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.core.RedisBloomFilterItem;
import com.opensource.redisaux.bloomfilter.support.GetBloomFilterField;
import com.opensource.redisaux.bloomfilter.support.SFunction;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RedisBloomFilter {


    private final Map<Class, RedisBloomFilterItem> bloomFilterMap;


    public RedisBloomFilter(Map<Class, RedisBloomFilterItem> bloomFilterMap) {
        this.bloomFilterMap = bloomFilterMap;
    }


    public void add(String keyPrefix, String key, Object member) {
        add(keyPrefix, key, member, 1000L, 0.03);
    }

    /**
     * 通过解析lambda获取信息
     *
     * @param sFunction
     * @param member
     * @param <T>
     */
    public <T, R> void add(SFunction<T> sFunction, R member) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
            add(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), member, bloomFilterInfo.getExceptionInsert(), bloomFilterInfo.getFpp());


    }

    public <R> void add(String keyPrefix, String key, R member, Long exceptedInsertions) {
        add(keyPrefix, key, member, exceptedInsertions, 0.03);
    }

    public <R> void add(String keyPrefix, String key, R member, Long exceptedInsertions, Double fpp) {
        Class clzz = member.getClass();
        Object res = member;
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        if (filter == null) {
            res = JSON.toJSONBytes(member, SerializerFeature.NotWriteDefaultValue);
            filter = bloomFilterMap.get(Byte.class);
        }
        String keyName =checkKey(keyPrefix,key) ;
        filter.put(keyName, res, exceptedInsertions, fpp);
    }

    public <T, R> void addAll(SFunction<T> sFunction, List<R> members) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
            addAll(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), bloomFilterInfo.getExceptionInsert(), bloomFilterInfo.getFpp(), members);
    }

    public <R> void addAll(String keyPrefix, String key, List<R> members) {
        this.addAll(keyPrefix, key, 1000L, 0.03, members);
    }

    public <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, List<R> members) {
        this.addAll(keyPrefix, key, exceptedInsertions, 0.03, members);
    }

    public <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, Double fpp, List<R> members) {
        if (members.isEmpty()) {
            throw new RedisAuxException("参数有误!");
        }
        String keyName =checkKey(keyPrefix,key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List resList = getAddList(members, filter);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.putAll(keyName, exceptedInsertions, fpp, resList);
    }






    public <R> boolean mightContain(String keyPrefix, String key, R member) {
        String keyName = checkKey(keyPrefix,key);
        Class clzz = member.getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        Object res = member;
        if (filter == null) {
            res = JSON.toJSONBytes(member, SerializerFeature.NotWriteDefaultValue);
            filter = bloomFilterMap.get(Byte.class);
        }
        return filter.mightContain(keyName, member);
    }

    public <T, R> boolean mightContain(SFunction<T> sFunction, R member) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        return mightContain(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), member);
    }

    public <T, R> List<Boolean> mightContains(SFunction<T> sFunction, List<R> members) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        return mightContains(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), members);
    }

    public <R> List<Boolean> mightContains(String keyPrefix, String key, List<R> members) {
        if (members.isEmpty()) {
            return null;
        }
        String keyName = checkKey(keyPrefix,key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List resList = getAddList(members, filter);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        return filter.mightContains(keyName, resList);
    }


    public <T> void remove(SFunction<T> sFunction) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        remove(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName());

    }
    public void remove(String keyPrefix, String key) {
        String keyname = checkKey(keyPrefix, key);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.remove(keyname);
        }
    }
    /**
     * 这里因为不同类对应不同的item存放，所以
     *
     * @param keys
     * @param keyPrefix
     */
    public void removeAll(Collection<String> keys, String keyPrefix) {
        List<String> keyList = keys.stream().map(e -> checkKey(keyPrefix,e)).collect(Collectors.toList());
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.removeAll(keyList);
        }
    }

    public <T> void reset(SFunction<T> sFunction) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = GetBloomFilterField.resolveFieldName(sFunction);
        reset(bloomFilterInfo.getKeyPrefix(),bloomFilterInfo.getKeyName());
    }

    public  void reset(String keyPrefix,String keyName){
        keyName = checkKey(keyPrefix, keyName);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.reset(keyName);
        }
    }


    private static <R> List getAddList(List<R> members, RedisBloomFilterItem filter) {
        List<Object> resList = new ArrayList<>(members.size());
        for (R member : members) {
            Object res = member;
            if (filter == null) {
                res = JSON.toJSONBytes(member, SerializerFeature.NotWriteDefaultValue);
            }
            resList.add(res);
        }
        return resList;
    }

    private GetBloomFilterField.BloomFilterInfo check(SFunction sFunction) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = GetBloomFilterField.resolveFieldName(sFunction);
        if (bloomFilterInfo == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        }
        return bloomFilterInfo;
    }
    private String checkKey(String prefix,String key){
        return StringUtils.isEmpty(prefix)?key:CommonUtil.getKeyName(prefix, key);
    }

}