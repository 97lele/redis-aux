package com.opensource.redisaux.bloomfilter.support.expire;


/**
 * @author lulu
 * @Date 2020/1/11 20:17
 */
public interface KeyExpirePublisher {
    void addListener(KeyExpireListener listener);

    void removeListener(KeyExpireListener listener);

    void notifyListener(String key);
}
