package com.xl.redisaux.transport.dispatcher;

import com.xl.redisaux.transport.common.RemoteAction;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.Data;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

@Data
public class ActionFuture {
    private Long start;
    private Long handleMills;
    private Integer requestId;
    private Integer code;
    private Object obj;
    private RemoteAction response;
    private Throwable e;

    public ActionFuture(RemoteAction request) {
        this.start = System.currentTimeMillis();
        this.requestId = request.getRequestId();
        this.code = request.getActionCode();
        this.obj = request.getBody();
    }

    private final Sync sync = new Sync();

    public void success(RemoteAction response) {
        this.response = response;
        sync.release(1);
        this.handleMills = System.currentTimeMillis() - start;
    }

    public void fail(Throwable e) {
        this.e = e;
        sync.release(1);
        this.handleMills = System.currentTimeMillis() - start;
    }


    public boolean isDone() {
        return sync.isDone();
    }

    public RemoteAction get(long timeout, TimeUnit unit) throws InterruptedException {
        //超时获取
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            return get();
        }
        return null;
    }

    public RemoteAction get() {
        if (isDone()) {
            return this.response;
        }
        return null;
    }


    /**
     * 继承同步器，这里只是用来自旋改变状态，根据state来实现，state初始为0
     */
    private static final class Sync extends AbstractQueuedSynchronizer {

        /**
         * 尝试获取锁,如果获取不了，加入同步队列，阻塞自己，只由同步队列的头自旋获取锁
         * 当状态为1，即有结果返回时可以获取锁进行后续操作,设置result
         * 这里只有一个节点，会不断自选尝试获取锁
         *
         * @param arg
         * @return
         */
        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == 1;
        }

        /**
         * 用于远端有返回时，设置状态变更
         * 从头唤醒同步队列的队头下一个等待的节点，如果下一个节点为空，则从队尾唤醒
         *
         * @param arg
         * @return
         */
        @Override
        protected boolean tryRelease(int arg) {
            //把状态设置为1，给tryAcquire获取锁进行操作
            return getState() != 0 || compareAndSetState(0, 1);
        }

        public boolean isDone() {
            return getState() == 1;
        }
    }
}
