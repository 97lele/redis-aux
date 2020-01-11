package com.opensource.redisaux.bloomfilter.support.observer;

import com.opensource.redisaux.bloomfilter.core.FunnelEnum;
import com.opensource.redisaux.bloomfilter.core.WatiForDeleteKey;
import org.springframework.beans.factory.InitializingBean;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CheckTask extends Thread implements KeyExpirePublisher, InitializingBean {
    private List<KeyExpireListener> listeners = new ArrayList<>();
    private PriorityQueue<WatiForDeleteKey> priorityQueue;
    private volatile Boolean run = true;


    public CheckTask() {
        this.priorityQueue = new PriorityQueue<>();
    }

    @Override
    public void run() {
        while (run) {
            if (priorityQueue.isEmpty()) {
                try {
                    TimeUnit.SECONDS.sleep(5L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                WatiForDeleteKey peek = priorityQueue.peek();
                long dispearTime = peek.getExistTime() + peek.getStartTime();
                if (dispearTime < System.currentTimeMillis()+10) {
                    WatiForDeleteKey poll = priorityQueue.poll();
                    notifyListener(poll.getKey());
                }

            }

        }
    }

    public synchronized void addExpireKey(WatiForDeleteKey watiForDeleteKey){
        priorityQueue.add(watiForDeleteKey);
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
    public void notifyListener(String key) {
        CountDownLatch countDownLatch = new CountDownLatch(FunnelEnum.values().length);
        listeners.forEach(e -> e.removeKey(key, countDownLatch));
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.start();
    }
    @PreDestroy
    public void stopRun() {
        this.run = Boolean.FALSE;
    }
}