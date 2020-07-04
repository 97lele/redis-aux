package com.opensource.redisaux.common.utils.qps;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lulu
 * @Date 2020/7/4 23:20
 */
public class WindowQpsCounter implements QpsCounter {

    /**
     * 槽位的数量
     */
    private int bucketSize;
    /**
     * 时间片，单位毫秒
     */
    private int window;
    /**
     * 用于判断是否可跳过锁争抢
     */
    private int timeSliceUsedToCheckIfPossibleToBypass;
    /**
     * 槽位
     */
    private Bucket[] buckets;
    /**
     * 目标槽位的位置
     */
    private volatile Integer targetBucketPosition;
    /**
     * 接近目标槽位最新更新时间的时间
     */
    private volatile Long latestPassedTimeCloseToTargetBucket;
    /**
     * 进入下一个槽位时使用的锁
     */
    private ReentrantLock enterNextBucketLock;


    private int interval;

    /**
     * 默认统计1分钟,分为10个桶
     */
    public WindowQpsCounter() {
        this(10, 1, TimeUnit.MINUTES);
    }

    /**
     * 默认分为10个桶
     * @param interval
     * @param timeUnit
     */
    public WindowQpsCounter(int interval, TimeUnit timeUnit) {
        this(10, interval, timeUnit);
    }

    /**
     * 根据桶数量和统计周期确定单个时间片长度
     * @param bucketSize
     * @param interval
     * @param timeUnit
     */
    public WindowQpsCounter(int bucketSize, int interval, TimeUnit timeUnit) {
        this.interval = (int) TimeUnit.MILLISECONDS.convert(interval, timeUnit);
        this.bucketSize = bucketSize;
        this.window = this.interval / bucketSize;
        this.latestPassedTimeCloseToTargetBucket = System.currentTimeMillis() - (2 * window);
        this.targetBucketPosition = null;
        this.bucketSize = bucketSize;
        this.enterNextBucketLock = new ReentrantLock();
        this.buckets = new Bucket[bucketSize];
        this.timeSliceUsedToCheckIfPossibleToBypass = 2 * window;
        for (int i = 0; i < bucketSize; i++) {
            this.buckets[i] = new Bucket();
        }
    }

    @Override
    public void pass(boolean success) {
        long passTime = System.currentTimeMillis();
        if (targetBucketPosition == null) {
            targetBucketPosition = (int) (passTime / window) % bucketSize;
        }
        Bucket currentBucket = buckets[targetBucketPosition];
        //判断当前与最近一次访问差距是否大于单个窗口
        if (passTime - latestPassedTimeCloseToTargetBucket >= window) {
            //如果锁被占用并且目标桶的最近更新时间小于可容忍时间差距，把计数放到当前桶里面，目的是减少竞争
            if (enterNextBucketLock.isLocked() && (passTime - latestPassedTimeCloseToTargetBucket) < timeSliceUsedToCheckIfPossibleToBypass) {

            }
            else {
                try {
                    enterNextBucketLock.lock();
                    //如果当前时间与最近一次访问时间差大于单个时间片
                    if (passTime - latestPassedTimeCloseToTargetBucket >= window) {
                        //获取下个桶的位置，循环数组取余
                        int nextTargetBucketPosition = (int) (passTime / window) % bucketSize;
                        Bucket nextBucket = buckets[nextTargetBucketPosition];
                        //更新最近一次访问时间，如果桶不相同，则重置计数，
                        if (nextBucket.equals(currentBucket)) {
                            if (passTime - latestPassedTimeCloseToTargetBucket >= window) {
                                latestPassedTimeCloseToTargetBucket = passTime;
                            }
                        } else {
                            nextBucket.reset(passTime);
                            targetBucketPosition = nextTargetBucketPosition;
                            latestPassedTimeCloseToTargetBucket = passTime;
                        }
                        //计数
                        nextBucket.pass(success);
                        return;
                    } else {
                        //否则更新当前桶坐标
                        currentBucket = buckets[targetBucketPosition];
                    }
                } finally {
                    enterNextBucketLock.unlock();
                }
            }
        }
        currentBucket.pass(success);

    }

    /**
     * 计算总和
     * @return
     */
    @Override
    public Map<String, String> getSum() {
        long success = 0;
        long fail = 0;
        long now = System.currentTimeMillis();
        Bucket[] buckets = this.buckets;
        for (int i = 0; i < buckets.length; i++) {
            Bucket bucket = buckets[i];
            //如果当前桶的访问时间和当前时间差距小于单个周期
            if ((now - bucket.latestPassedTime) <= this.interval) {
                success += bucket.getSuccessCount();
                fail += bucket.getFailCount();
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("success", success + "");
        map.put("fail", fail + "");
        map.put("total", fail + success + "");
        return map;
    }

    @Override
    public void setInterval(int interval, TimeUnit unit, int bucketSize) {
        this.interval = (int) TimeUnit.MILLISECONDS.convert(interval, unit);
        this.window = this.interval / bucketSize;
        this.bucketSize = bucketSize;
        this.timeSliceUsedToCheckIfPossibleToBypass = 2 * this.window;
    }



    /**
     * 主要存放成功和失败的次数
     */
    private static class Bucket {

        private Long latestPassedTime;

        private LongAdder successCount;

        private LongAdder failCount;

        public Bucket() {
            this.latestPassedTime = System.currentTimeMillis();
            this.successCount = new LongAdder();
            this.failCount = new LongAdder();
        }


        public void pass(boolean success) {
            if (success) {
                successCount.add(1);
            } else {
                failCount.add(1);
            }
        }

        public long getSuccessCount() {
            return successCount.sum();
        }

        public long getFailCount() {
            return failCount.sum();
        }

        public void reset(long latestPassedTime) {
            this.successCount.reset();
            this.failCount.reset();
            this.latestPassedTime = latestPassedTime;
        }
    }
}
