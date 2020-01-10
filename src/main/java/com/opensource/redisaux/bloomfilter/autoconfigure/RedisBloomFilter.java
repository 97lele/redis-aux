package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.core.RedisBloomFilterItem;
import com.opensource.redisaux.bloomfilter.support.GetBloomFilterField;
import com.opensource.redisaux.bloomfilter.support.SFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        Object[] objects = GetBloomFilterField.resolveFieldName(sFunction);
        if (objects == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        } else {
            add(objects[0].toString(), objects[1].toString(), member, (Long) objects[2], (Double) objects[3]);
        }

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
        String keyName = CommonUtil.getKeyName(keyPrefix, key);
        filter.put(keyName, res, exceptedInsertions, fpp);
    }

    public <T, R> void addAll(SFunction<T> sFunction, List<R> members) {
        Object[] objects = GetBloomFilterField.resolveFieldName(sFunction);

        if (objects == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        } else {
            addAll(objects[0].toString(), objects[1].toString(), (Long) objects[2], (Double) objects[3], members);
        }
    }
    public <R> void addAll(String keyPrefix, String key,  List<R> members){
        this.addAll(keyPrefix,key,1000L,0.03,members);
    }

    public <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, List<R> members){
        this.addAll(keyPrefix,key,exceptedInsertions,0.03,members);
    }

    public <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, Double fpp, List<R> members) {
        if (members.isEmpty()) {
            throw new RedisAuxException("参数有误!");
        }
        String keyName = CommonUtil.getKeyName(keyPrefix, key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List resList = getAddList(members, filter);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.putAll(keyName, exceptedInsertions, fpp, resList);
    }

    public <T> void remove(SFunction<T> sFunction) {
        Object[] objects = GetBloomFilterField.resolveFieldName(sFunction);
        if (objects == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        } else {
            remove(objects[0].toString(), objects[1].toString());
        }
    }

    public <R> boolean mightContain(String keyPrefix, String key, R member) {
        String keyName = CommonUtil.getKeyName(keyPrefix, key);
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
        Object[] objects = GetBloomFilterField.resolveFieldName(sFunction);
        if (objects == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        }
        return mightContain(objects[0].toString(), objects[1].toString(), member);

    }

    public <T, R> List<Boolean> mightContains(SFunction<T> sFunction, List<R> members) {
        Object[] objects = GetBloomFilterField.resolveFieldName(sFunction);
        if (objects == null) {
            throw new RedisAuxException("请检查注解配置是否正确!");
        }
        return this.mightContains(objects[0].toString(), objects[1].toString(), members);
    }

    public <R> List<Boolean> mightContains(String keyPrefix, String key, List<R> members) {
        if (members.isEmpty()) {
            return null;
        }
        String keyName = CommonUtil.getKeyName(keyPrefix, key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List resList = getAddList(members, filter);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        return filter.mightContains(keyName, resList);
    }

    public void remove(String keyPrefix, String key) {
        String keyname = CommonUtil.getKeyName(keyPrefix, key);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.remove(keyname);
        }
    }

    public void removeAll(Collection<String> keys, String keyPrefix) {
        keys.forEach(e -> CommonUtil.getKeyName(keyPrefix, e));
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.removeAll(keys);
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

}