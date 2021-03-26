package com.xl.redisaux.common.utils.lock;

import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.common.utils.NamedThreadFactory;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author tanjl11
 * @date 2020/10/14 15:07
 */
public class LockUtil {

    private StringRedisTemplate lockTemplate;

    public LockUtil(StringRedisTemplate lockTemplate) {
        this.lockTemplate = lockTemplate;
    }

    private volatile DefaultRedisScript<Boolean> lockScript;

    private volatile DefaultRedisScript<Long> upLockScript;

    private volatile DefaultRedisScript<Boolean> extendScript;

    private final static long INTERVAL_CHECK_EXPIRE = 30;
    private final String PREFIX = "redisLock:";

    private HashedWheelTimer wheelTimer;

    private DefaultRedisScript getExtendScript() {
        if (Objects.isNull(extendScript)) {
            synchronized (LockUtil.class) {
                if (Objects.isNull(extendScript)) {
                    wheelTimer = new HashedWheelTimer(new NamedThreadFactory("watch-dog-thread-pool",true),500, TimeUnit.MILLISECONDS, 1024);
                    extendScript = new DefaultRedisScript<>();
                    extendScript.setResultType(Boolean.class);
                    extendScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("Extend.lua")));
                }
            }
        }
        return extendScript;
    }

    private DefaultRedisScript getLockScript() {
        if (Objects.isNull(lockScript)) {
            synchronized (LockUtil.class) {
                if (Objects.isNull(lockScript)) {
                    lockScript = new DefaultRedisScript();
                    lockScript.setResultType(Boolean.class);
                    lockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("Lock.lua")));
                }
            }
        }
        return lockScript;
    }

    private DefaultRedisScript getUnLockScript() {
        if (Objects.isNull(upLockScript)) {
            synchronized (LockUtil.class) {
                if (Objects.isNull(upLockScript)) {
                    upLockScript = new DefaultRedisScript();
                    upLockScript.setResultType(Long.class);
                    upLockScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("UnLock.lua")));
                }
            }
        }
        return upLockScript;
    }


    /**
     * @param key
     * @param time
     * @param awaitTime
     * @param retryCount
     * @param sleepTime
     * @return
     */
    public Boolean tryLockInSeconds(String key, long time, long awaitTime, long retryCount, long sleepTime) {
        return tryLockInSeconds(key, time, TimeUnit.SECONDS, awaitTime, TimeUnit.SECONDS, retryCount, sleepTime, TimeUnit.SECONDS);
    }

    /**
     * 超时可重入锁
     *
     * @param key        锁名
     * @param expireTime 锁过期时间
     * @param unit       锁过期时间单位
     * @param awaitTime  锁等待超时时间
     * @param awaitUnit  锁
     * @param retryCount 最大获取次数
     * @param sleepTime  每次获取失败后的睡眠时间
     * @param sleepUnit  睡眠单位
     * @return
     */
    public Boolean tryLockInSeconds(String key, long expireTime, TimeUnit unit, long awaitTime, TimeUnit awaitUnit, long retryCount, long sleepTime, TimeUnit sleepUnit) {
        //先获取一次
        Boolean isLock = tryLock(key, expireTime, unit);
        //不行再获取
        if (!isLock) {
            long nanos = awaitUnit.toNanos(awaitTime);
            final long deadline = System.nanoTime() + nanos;
            int count = 0;
            while (true) {
                nanos = deadline - System.nanoTime();
                //超时
                if (nanos <= 0L) {
                    return false;
                }
                isLock = tryLock(key, expireTime, unit);
                if (isLock) {
                    return true;
                }
                //如果大于最大获取次数或者线程被中断
                if (count++ > retryCount || Thread.interrupted()) {
                    return false;
                }
                //阻塞
                LockSupport.parkNanos(sleepUnit.toNanos(sleepTime));
            }
        }
        return true;
    }

    /**
     * 无限延长方法
     *
     * @param key
     * @return
     */
    public Boolean tryLock(String key) {
        //先锁一次
        Boolean lock = tryLock(key, INTERVAL_CHECK_EXPIRE, TimeUnit.SECONDS);
        if (lock) {
            DefaultRedisScript extendScript = getExtendScript();
            wheelTimer.newTimeout(new TimerTask() {
                @Override
                public void run(Timeout timeout) throws Exception {
                    //延迟0.8后开始校验，是否存在锁，存在就延期,不存在就删除,
                    Boolean isExtend = (Boolean) lockTemplate.execute(extendScript, Collections.singletonList(PREFIX + key), String.valueOf(TimeUnit.MILLISECONDS.convert(INTERVAL_CHECK_EXPIRE, TimeUnit.SECONDS)));
                    if (isExtend) {
                        wheelTimer.newTimeout(this, (long) (INTERVAL_CHECK_EXPIRE * 0.8), TimeUnit.SECONDS);
                    } else {
                        timeout.cancel();
                    }
                }
            }, (long) (INTERVAL_CHECK_EXPIRE * 0.8), TimeUnit.SECONDS);
            return true;
        }
        return false;
    }

    /**
     * 按照秒来锁
     *
     * @param key
     * @param time
     * @return
     */
    public Boolean tryLock(String key, long time) {
        return tryLock(key, time, TimeUnit.SECONDS);
    }

    /**
     * key可能不一样，但是threadSign是必须有的
     * @param key
     * @param expireTime
     * @param unit
     * @return
     */
    public Boolean tryLock(String key, long expireTime, TimeUnit unit) {
        String nK = PREFIX + key;
        String threadSign = RedisLockInfoHolder.get();
        //设置线程标志
        if (Objects.isNull(threadSign)) {
            String uuid = UUID.randomUUID().toString();
            String value = uuid + Thread.currentThread().getId();
            threadSign = value;
            RedisLockInfoHolder.setValue(threadSign);
        }
        long millis = unit.toMillis(expireTime);
        //要使用stringRedisTemplate才可以设置上
        Boolean isLock = (Boolean) lockTemplate.execute(getLockScript(), Collections.singletonList(nK), threadSign, String.valueOf(millis));
        if(isLock){
            RedisLockInfoHolder.push(key);
        }
        return isLock;
    }

    /**
     * 解锁
     *
     * @param key
     * @return
     */
    public UnLockStatus unLock(String key) {
        String nK = PREFIX + key;
        String threadSign = RedisLockInfoHolder.get();
        Long result = (Long) lockTemplate.execute(getUnLockScript(), Collections.singletonList(nK), threadSign);
        //如果成功解锁，清除线程标志
        if (Objects.equals(result, UnLockStatus.UNLOCK_SUCCESS.getStatus())) {
            RedisLockInfoHolder.clear();
        }
        return UnLockStatus.getByStatus(result);
    }

    private static class RedisLockInfoHolder {
        static ThreadLocal<String> holder = new ThreadLocal<>();
        static ThreadLocal<Stack<String>> keyHolder=ThreadLocal.withInitial(Stack::new);

        private static void clear() {
            poll();
            if(keyHolder.get().isEmpty()){
                holder.remove();
            }
        }

        private static String get() {
            return holder.get();
        }

        private static String peek(){
           return keyHolder.get().isEmpty()?null:keyHolder.get().peek();
        }

        private static void push(String key){
            String peek = peek();
            //同一个key不入栈
            if(peek==null||!peek.equals(key)){
                keyHolder.get().push(key);
            }
       }

        private static String poll(){
            return keyHolder.get().isEmpty()?null:keyHolder.get().pop();
        }

        private static void setValue(String value) {
            holder.set(value);
        }
    }
}
