package com.opensource.redisaux.bloomfilter.support.expire;

/**
 * @author lulu
 * @Date 2020/1/11 20:18
 * 对应的Redis和本地缓存都要清理
 */
public interface KeyExpireListener {
    void removeKey(String ke);
}
