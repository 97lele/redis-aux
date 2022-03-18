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
        sync.tryRelease(-1);
        this.handleMills=System.currentTimeMillis()-start;
    }

    public void fail(Throwable e) {
        this.e = e;
        sync.tryRelease(-1);
        this.handleMills=System.currentTimeMillis()-start;
    }

    public RemoteAction get(Integer sec, TimeUnit timeUnit) throws InterruptedException {
        sync.tryAcquireNanos(0, timeUnit.toNanos(sec));
        return response;
    }

    public RemoteAction get() {
        sync.tryAcquire(0);
        return response;
    }


    private final static class Sync extends AbstractQueuedSynchronizer {

        public Sync() {
            setState(1);
        }

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == 0;
        }

        @Override
        protected boolean tryRelease(int arg) {
            return compareAndSetState(1, 0);
        }
    }
}
