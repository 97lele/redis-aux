package com.opensource.redisaux.bloomfilter.core.filter;

import com.opensource.redisaux.CommonUtil;
import com.opensource.redisaux.RedisAuxException;
import com.opensource.redisaux.bloomfilter.support.GetBloomFilterField;
import com.opensource.redisaux.bloomfilter.support.SFunction;
import org.springframework.util.StringUtils;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeUnit;

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
        add(bloomFilterInfo.getKeyPrefix(),
                bloomFilterInfo.getKeyName(),
                bloomFilterInfo.getExceptionInsert(),
                bloomFilterInfo.getFpp(),
                bloomFilterInfo.getTimeout(),
                bloomFilterInfo.getTimeUnit(),
                bloomFilterInfo.isEnableGrow(),
                bloomFilterInfo.getGrowRate(),
                member);
    }

    public <R> void add(AddCondition addCondition, R member) {
        InnerInfo condition = addCondition.build();
        addCondition.clear();
        add(condition.getKeyPrefix(),
                condition.getKeyName(),
                condition.getExceptionInsert(),
                condition.getFpp(),
                condition.getTimeout(),
                condition.getTimeUnit(),
                condition.isEnableGrow(),
                condition.getGrowRate(),
                member
        );
    }

    private <R> void add(String keyPrefix, String key, long exceptedInsertions, double fpp, long timeout, TimeUnit timeUnit, boolean enableGrow, double growRate, R member) {
        Class clzz = member.getClass();
        Object res = member;
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        String keyName = checkKey(keyPrefix, key);
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.put(keyName, res, exceptedInsertions, fpp, timeout, timeUnit, enableGrow, growRate);
    }

    public <T, R> void addAll(SFunction<T> sFunction, List<R> members) {
        GetBloomFilterField.BloomFilterInfo bloomFilterInfo = check(sFunction);
        addAll(bloomFilterInfo.getKeyPrefix(),
                bloomFilterInfo.getKeyName(),
                bloomFilterInfo.getExceptionInsert(),
                bloomFilterInfo.getFpp(),
                bloomFilterInfo.getTimeout(),
                bloomFilterInfo.getTimeUnit(),
                bloomFilterInfo.isEnableGrow(),
                bloomFilterInfo.getGrowRate(),
                members);
    }

    public <R> void addAll(AddCondition addCondition, List<R> members) {
        InnerInfo innerInfo = addCondition.build();
        addCondition.clear();
        this.addAll(
                innerInfo.getKeyPrefix(),
                innerInfo.getKeyName(),
                innerInfo.getExceptionInsert(),
                innerInfo.getFpp(),
                innerInfo.getTimeout(),
                innerInfo.getTimeUnit(),
                innerInfo.isEnableGrow(),
                innerInfo.getGrowRate(),
                members
        );
    }

    private <R> void addAll(String keyPrefix, String key, Long exceptedInsertions, Double fpp, long timeout, TimeUnit timeUnit, boolean enableGrow, double growRate, List<R> members) {
        if (members.isEmpty()) {
            throw new RedisAuxException("参数有误!");
        }
        String keyName = checkKey(keyPrefix, key);
        Class clzz = members.get(0).getClass();
        RedisBloomFilterItem filter = bloomFilterMap.get(clzz);
        List<Object> resList = new ArrayList(members.size());
        for (R member : members) {
            resList.add(member);
        }
        if (filter == null) {
            filter = bloomFilterMap.get(Byte.class);
        }
        filter.putAll(keyName, exceptedInsertions, fpp, resList, timeout, timeUnit, enableGrow, growRate);
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
        if (filter == null) {
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
        List<Object> resList = new ArrayList(members.size());
        for (R member : members) {
            resList.add(member);
        }
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

        List<String> keyList = new LinkedList();
        for (String key : keys) {
            keyList.add(checkKey(keyPrefix,key));
        }
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
        expireCondition.clear();
        expire(condition.getKeyPrefix(), condition.getKeyName(), condition.getTimeout(), condition.getTimeUnit());
    }

    private void expire(String keyPrefix, String keyName, long timeout, TimeUnit timeUnit) {
        keyName = checkKey(keyPrefix, keyName);
        for (RedisBloomFilterItem filter : bloomFilterMap.values()) {
            filter.expire(keyName, timeout, timeUnit);
        }
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

    @PreDestroy
    protected void destory() {
        for (RedisBloomFilterItem value : this.bloomFilterMap.values()) {
            value.clear();
        }
        this.bloomFilterMap.clear();
    }


}