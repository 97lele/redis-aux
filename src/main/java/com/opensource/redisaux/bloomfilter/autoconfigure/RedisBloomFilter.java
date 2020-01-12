package com.opensource.redisaux.bloomfilter.autoconfigure;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.core.RedisBloomFilterItem;
import com.opensource.redisaux.bloomfilter.support.GetBloomFilterField;
import com.opensource.redisaux.bloomfilter.support.SFunction;
import com.opensource.redisaux.bloomfilter.support.builder.AddCondition;
import com.opensource.redisaux.bloomfilter.support.builder.BaseCondition;
import com.opensource.redisaux.bloomfilter.support.builder.ExpireCondition;
import com.opensource.redisaux.bloomfilter.support.builder.InnerInfo;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisBloomFilter {


    private final Map<Class, RedisBloomFilterItem> bloomFilterMap;


    public RedisBloomFilter(Map<Class, RedisBloomFilterItem> bloomFilterMap) {
        this.bloomFilterMap = bloomFilterMap;
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
        add(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), member, bloomFilterInfo.getExceptionInsert(), bloomFilterInfo.getFpp(), bloomFilterInfo.getTimeout(), bloomFilterInfo.getTimeUnit(),bloomFilterInfo.getGrowRate());
    }

    public <R> void add(AddCondition addCondition, R member) {
        InnerInfo condition = addCondition.build();
        add(condition.getKeyPrefix(), condition.getKeyName(), member, condition.getExceptionInsert(), condition.getFpp(), condition.getTimeout(), condition.getTimeUnit(),condition.getGrowRate());
    }

    private <R> void add(String keyPrefix, String key, R member, long exceptedInsertions, double fpp, long timeout, TimeUnit timeUnit,double growRate) {
        Class clzz = member.getClass();
        Object res = member;
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        String keyName = checkKey(keyPrefix, key);
        if (filter == null) {
            res = JSON.toJSONBytes(member, SerializerFeature.NotWriteDefaultValue);
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.put(keyName, res, exceptedInsertions, fpp, timeout, timeUnit,growRate);
    }

    public <T, R> void addAll(SFunction<T> sFunction, List<R> members) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        addAll(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), bloomFilterInfo.getExceptionInsert(), bloomFilterInfo.getFpp(), members, bloomFilterInfo.getTimeout(), bloomFilterInfo.getTimeUnit(),bloomFilterInfo.getGrowRate());
    }

    public <R> void addAll(AddCondition addCondition, List<R> members) {
        InnerInfo innerInfo = addCondition.build();
        this.addAll(
                innerInfo.getKeyPrefix(),
                innerInfo.getKeyName(),
                innerInfo.getExceptionInsert(),
                innerInfo.getFpp(),
                members,
                innerInfo.getTimeout(),
                innerInfo.getTimeUnit(),
                innerInfo.getGrowRate()
        );
    }

    private <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, Double fpp, List<R> members, long timeout, TimeUnit timeUnit,double growRate) {
        if (members.isEmpty()) {
            throw new RedisAuxException("参数有误!");
        }
        String keyName = checkKey(keyPrefix, key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List resList = getAddList(members, filter);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.putAll(keyName, exceptedInsertions, fpp, resList, timeout, timeUnit,growRate);
    }

    public <R> boolean mightContain(BaseCondition queryCondition, R member) {
        InnerInfo build = queryCondition.build();
        return mightContain(build.getKeyPrefix(), build.getKeyName(), member);
    }

    public <T, R> boolean mightContain(SFunction<T> sFunction, R member) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        return mightContain(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), member);
    }

    private <R> boolean mightContain(String keyPrefix, String key, R member) {
        String keyName = checkKey(keyPrefix, key);
        Class clzz = member.getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        Object res = member;
        if (filter == null) {
            res = JSON.toJSONBytes(member, SerializerFeature.NotWriteDefaultValue);
            filter = bloomFilterMap.get(Byte.class);
        }
        return filter.mightContain(keyName, member);
    }


    public <T, R> List<Boolean> mightContains(SFunction<T> sFunction, List<R> members) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        return mightContains(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName(), members);
    }

    public <R> List<Boolean> mightContains(BaseCondition queryCondition, List<R> members) {
        InnerInfo build = queryCondition.build();
        return mightContains(build.getKeyPrefix(), build.getKeyName(), members);
    }

    private <R> List<Boolean> mightContains(String keyPrefix, String key, List<R> members) {
        if (members.isEmpty()) {
            return null;
        }
        String keyName = checkKey(keyPrefix, key);
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

    public void remove(BaseCondition deleteCondition) {
        InnerInfo build = deleteCondition.build();
        remove(build.getKeyPrefix(), build.getKeyName());
    }


    private void remove(String keyPrefix, String key) {
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
        List<String> keyList = keys.stream().map(e -> checkKey(keyPrefix, e)).collect(Collectors.toList());
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.removeAll(keyList);
        }
    }

    public void removeAll(Collection<String> keys) {
        removeAll(keys, null);
    }


    public <T> void reset(SFunction<T> sFunction) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = GetBloomFilterField.resolveFieldName(sFunction);
        reset(bloomFilterInfo.getKeyPrefix(), bloomFilterInfo.getKeyName());
    }

    public void reset(BaseCondition resetCondition) {
        InnerInfo build = resetCondition.build();
        reset(build.getKeyPrefix(), build.getKeyName());
    }


    private void reset(String keyPrefix, String keyName) {
        keyName = checkKey(keyPrefix, keyName);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.reset(keyName);
        }
    }

    public void expire(ExpireCondition expireCondition) {
        InnerInfo condition = expireCondition.build();
        expire(condition.getKeyPrefix(), condition.getKeyName(), condition.getTimeout(), condition.getTimeUnit());
    }

    private void expire(String keyPrefix, String keyName, long timeout, TimeUnit timeUnit) {
        keyName = checkKey(keyPrefix, keyName);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.expire(keyName, timeout, timeUnit);
        }
    }

    private static <R> List getAddList(List<R> members, RedisBloomFilterItem filter) {
        List<Object> resList = new ArrayList(members.size());
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

    private String checkKey(String prefix, String key) {
        return StringUtils.isEmpty(prefix) ? key : CommonUtil.getKeyName(prefix, key);
    }



}