package com.xl.redisaux.transport.dispatcher;

import com.xl.redisaux.transport.common.RemoteAction;
import io.netty.util.concurrent.DefaultPromise;

public class ActionFuture extends DefaultPromise<RemoteAction> {
    protected int requestId;
    public ActionFuture(RemoteAction remoteAction){
        if(remoteAction.isResponse()){
            throw new IllegalArgumentException("only request can be a future");
        }
        this.requestId=remoteAction.getRequestId();
    }
}
