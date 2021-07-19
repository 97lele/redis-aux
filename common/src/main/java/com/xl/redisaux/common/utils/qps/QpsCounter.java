package com.xl.redisaux.common.utils.qps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/7/4 10:59
 */
public interface QpsCounter {
    void pass(boolean success);

    Map<String, Object> getSum();

    void setInterval(int interval, TimeUnit unit, int bucketSize);
}
