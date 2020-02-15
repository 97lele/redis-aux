package com.opensource.redisaux.limiter.core;

import com.opensource.redisaux.limiter.annonations.TokenLimiter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: lele
 * @date: 2020/1/3 下午10:28
 */
@SuppressWarnings("unchecked")
public class TokenRateLimiter extends BaseRateLimiter {

    private RedisTemplate redisTemplate;

    private DefaultRedisScript redisScript;


    public TokenRateLimiter(RedisTemplate redisTemplate, DefaultRedisScript redisScript) {
        this.redisScript = redisScript;
        this.redisTemplate = redisTemplate;

    }

    @Override
    public Boolean canExecute(Annotation baseLimiter, String methodKey) {
        TokenLimiter tokenLimiter = (TokenLimiter) baseLimiter;
        TimeUnit rateUnit = tokenLimiter.rateUnit();
        double capacity = tokenLimiter.capacity();
        double need = tokenLimiter.need();
        double rate = tokenLimiter.rate();
        long l = rateUnit.toMillis(1);
        double millRate = rate / l;
        long last = System.currentTimeMillis();
        String methodName = tokenLimiter.fallback();
        boolean passArgs = tokenLimiter.passArgs();
        List<String> keyList = BaseRateLimiter.getKey(methodKey, methodName, passArgs);
        Object[] args = new Double[]{capacity, millRate, need, Double.valueOf(last)};
        Long waitMill = (Long) redisTemplate.execute(redisScript, keyList, args);
        if (waitMill.equals(-1L)) {
            return true;
        }
        if (tokenLimiter.isAbort()) {
            PendingNode pendingNode = new PendingNode(waitMill, args, keyList, last);
            while (!pendingNode.isDone()) {
                try {
                    //如果要等待的时间大于timeout，直接失败
                    int timeout = tokenLimiter.timeout();
                    if (timeout > 0 && tokenLimiter.timeoutUnit().toMillis(timeout) < pendingNode.getWaitMill()) {
                        //复杂对象帮助gc
                        pendingNode.setArgs(null);
                        pendingNode.setKeyList(null);
                        pendingNode = null;
                        return false;
                    }
                    //睡眠等待
                    TimeUnit.MILLISECONDS.sleep(pendingNode.getWaitMill() + 1);
                    //结束后，可以对参数进行重新赋值
                    long now = System.currentTimeMillis();
                    Object[] tryExecuteArgs = pendingNode.getArgs();
                    tryExecuteArgs[3] = Double.valueOf(now);
                    Long execute = (Long) redisTemplate.execute(redisScript, keyList, tryExecuteArgs);
                    if (execute.equals(-1L)) {
                        pendingNode.setDone();
                    } else {
                        //不成功就更新值
                        pendingNode.setWaitMill(execute);
                        pendingNode.setLastExecuteTime(now);
                        pendingNode.setArgs(tryExecuteArgs);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //返回前的资源处理
            pendingNode.setArgs(null);
            pendingNode.setKeyList(null);
            pendingNode = null;
            return true;
        }
        return false;
    }

    /**
     * 当阻塞时候启用的节点
     */
    class PendingNode {
        private Long waitMill;
        private Object[] args;
        private List<String> keyList;
        private long lastExecuteTime;
        private volatile boolean done = false;

        public PendingNode(long waitMill, Object[] args, List<String> keyList, long lastExecuteTime) {
            this.waitMill = waitMill;
            this.args = args;
            this.keyList = keyList;
            this.lastExecuteTime = lastExecuteTime;
        }

        public void setDone() {
            this.done = true;
        }

        public boolean isDone() {
            return done;
        }

        public long getLastExecuteTime() {
            return lastExecuteTime;
        }

        public void setLastExecuteTime(long lastExecuteTime) {
            this.lastExecuteTime = lastExecuteTime;
        }

        public long getWaitMill() {
            return waitMill;
        }

        public void setWaitMill(long waitMill) {
            this.waitMill = waitMill;
        }

        public Object[] getArgs() {
            return args;
        }

        public void setArgs(Object[] args) {
            this.args = args;
        }

        public List<String> getKeyList() {
            return keyList;
        }

        public void setKeyList(List<String> keyList) {
            this.keyList = keyList;
        }
    }
}