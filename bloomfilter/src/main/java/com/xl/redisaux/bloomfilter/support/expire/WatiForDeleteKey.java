package com.xl.redisaux.bloomfilter.support.expire;


import com.xl.redisaux.common.exceptions.RedisAuxException;

/**
 * @author lulu
 * @Date 2020/1/11 19:49
 */
public class WatiForDeleteKey implements Comparable {
    private final String key;
    private final long existTime;
    private final long startTime;

    public String getKey() {
        return key;
    }

    public long getExistTime() {
        return existTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public WatiForDeleteKey(String key, long existTime, long startTime,boolean mode) {
        this.key = key;
        this.existTime = existTime;
        this.startTime = startTime;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof WatiForDeleteKey) {
            long now = System.currentTimeMillis();
            //剩下的存活时间
            WatiForDeleteKey ot = (WatiForDeleteKey) o;
            long other = ot.existTime - (now - ot.startTime);

            long self = existTime - (now - startTime);
            //小顶堆，在上面是最小的，存活时间最少在上面
            return Long.compare(self, other);
        }
        throw new RedisAuxException("WaitForDeleteKey无法比对");
    }

    @Override
    public String toString() {
        return key + ":" + ((startTime + existTime) - System.currentTimeMillis());
    }
}
