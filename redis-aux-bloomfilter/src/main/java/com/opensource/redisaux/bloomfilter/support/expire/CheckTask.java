package com.opensource.redisaux.bloomfilter.support.expire;

import com.opensource.redisaux.common.BloomFilterConstants;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class CheckTask extends Thread implements KeyExpirePublisher, InitializingBean {
    private List<KeyExpireListener> listeners = new ArrayList();
    private PriorityBlockingQueue<WatiForDeleteKey> priorityQueue;
    private volatile Boolean run = true;
    private ThreadPoolExecutor executors;

    public CheckTask() {
        super("checkTask");
        this.priorityQueue = new PriorityBlockingQueue();
        executors = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS, new ArrayBlockingQueue(1024), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Override
    public void run() {
        while (run) {
            //当队列为空时，每5秒检测一次
            if (priorityQueue.isEmpty()) {
                try {
                    TimeUnit.SECONDS.sleep(BloomFilterConstants.CHECK_TASK_PER_SECOND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                //查看存活时间最小的那个节点
                WatiForDeleteKey peek = priorityQueue.peek();
                long dispearTime = 0;
                long now = 0;
                //针对键的存活时间较为密切的情况，一直弹出直到存活时间大于当前时间
                while (peek != null && (dispearTime = (peek.getStartTime() + peek.getExistTime())) <= (now = System.currentTimeMillis())) {
                    WatiForDeleteKey poll = priorityQueue.poll();
                    notifyListener(poll.getKey());
                    peek = priorityQueue.peek();
                }
                //等待下一次过期的时间
                try {
                    if (dispearTime > now) {
                        TimeUnit.MILLISECONDS.sleep(dispearTime - now);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public void addExpireKey(WatiForDeleteKey watiForDeleteKey) {
        priorityQueue.offer(watiForDeleteKey);
    }

    @Override
    public void addListener(KeyExpireListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(KeyExpireListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void notifyListener(final String key) {
//通过线程池提交删除任务
        for (final KeyExpireListener listener : listeners) {
            executors.submit(new Runnable() {
                @Override
                public void run() {
                    listener.removeKey(key);
                }
            });
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
    }

    @PreDestroy
    public void stopRun() {
        this.run = Boolean.FALSE;
        executors.shutdown();
    }
}