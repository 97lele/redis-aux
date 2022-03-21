package com.xl.redisaux.transport.dispatcher;

import com.xl.redisaux.transport.common.RemoteAction;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ResultHolder {

    private static Map<Integer, ActionFuture> remoteActionMap = new ConcurrentHashMap<>();

    public static ActionFuture putRequest(RemoteAction request) {
        int requestId = request.getRequestId();
        ActionFuture value = new ActionFuture(request);
        remoteActionMap.put(requestId, value);
        return value;
    }

    public static void onSuccess(RemoteAction remoteAction) {
        ActionFuture actionFuture = remoteActionMap.get(remoteAction.getRequestId());
        if (actionFuture != null) {
            actionFuture.success(remoteAction);
            log.trace("请求结束:{}", actionFuture);
            remoteActionMap.remove(remoteAction.getRequestId());
        }
    }

    public static void onFail(Throwable e, int requestId) {
        ActionFuture actionFuture = remoteActionMap.get(requestId);
        if (actionFuture != null) {
            actionFuture.fail(e);
            remoteActionMap.remove(requestId);
        }
    }
}
