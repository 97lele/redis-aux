package com.xl.redisaux.transport.dispatcher;

import com.xl.redisaux.transport.common.RemoteAction;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResultHolder {

    private static Map<Integer, ActionFuture> remoteActionMap = new ConcurrentHashMap<>();

    public static ActionFuture putRequest(RemoteAction request) {
        ActionFuture value = new ActionFuture(request);
        remoteActionMap.put(value.requestId, value);
        return value;
    }

    public static void onSuccess(RemoteAction remoteAction) {
        ActionFuture actionFuture = remoteActionMap.get(remoteAction.getRequestId());
        if (actionFuture != null) {
            actionFuture.setSuccess(remoteAction);
            remoteActionMap.remove(remoteAction.getRequestId());
        }
    }

    public static void onFail(Throwable e, int requestId) {
        ActionFuture actionFuture = remoteActionMap.get(requestId);
        if (actionFuture != null) {
            actionFuture.setFailure(e);
            remoteActionMap.remove(requestId);
        }
    }
}
