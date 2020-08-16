/*
package com.xl.redisaux.dashboard.controller;

import com.xl.redisaux.common.exceptions.RedisAuxException;
import com.xl.redisaux.common.vo.Result;
import com.xl.redisaux.dashboard.consts.PathConsts;
import com.xl.redisaux.dashboard.vo.*;
import com.xl.redisaux.transport.server.HeartBeatServerHandler;
import com.xl.redisaux.transport.server.SendRequest;
import com.xl.redisaux.transport.vo.NodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

*/
/**
 * @author lulu
 * @Date 2020/7/19 14:18
 *//*

@RestController
public class ManagerRuleController {
    @Autowired
    SendRequest sendRequest;


    @GetMapping("/getNodes")
    public Result<Collection<NodeVO>> nodeVOS() {
        return Result.success(HeartBeatServerHandler.nodeVOMap.values());
    }

    @PostMapping("/sendCommand/changeFunnel")
    public Result<String> send(@RequestBody ChangeFunnelConfig funnelConfig) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeFunnelConfig.class, funnelConfig);
            sendRequest.executeCommand(funnelConfig.ip, funnelConfig.port, PathConsts.prefix,PathConsts.FUNNELCONFIG, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(funnelConfig.groupId);
    }
    @PostMapping("/sendCommand/chaneIpRule")
    public Result<String> send(@RequestBody ChangeIpRule ipRule) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeIpRule.class, ipRule);
            sendRequest.executeCommand(ipRule.ip, ipRule.port,PathConsts.prefix, PathConsts.CHANGEIPRULE, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(ipRule.groupId);
    }
    @PostMapping("/sendCommand/changeLimiteMode")
    public Result<String> send(@RequestBody ChangeLimiteMode mode) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeLimiteMode.class, mode);
            sendRequest.executeCommand(mode.ip, mode.port, PathConsts.prefix,PathConsts.CHANGEMODE, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(mode.groupId);
    }
    @PostMapping("/sendCommand/changeTokenConfig")
    public Result<String> send(@RequestBody ChangeTokenConfig config) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeTokenConfig.class, config);
            sendRequest.executeCommand(config.ip, config.port, PathConsts.prefix,PathConsts.TOKENCONFIG, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(config.groupId);
    }
    @PostMapping("/sendCommand/changeUrlRule")
    public Result<String> send(@RequestBody ChangeUrlRule urlRule) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeUrlRule.class, urlRule);
            sendRequest.executeCommand(urlRule.ip, urlRule.port,PathConsts.prefix, PathConsts.CHANGERULE, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(urlRule.groupId);
    }
    @PostMapping("/sendCommand/changeWindowConfig")
    public Result<String> send(@RequestBody ChangeWindowConfig windowConfig) {
        try {
            Map<String, String> param = changeEntity2Map(ChangeWindowConfig.class, windowConfig);
            sendRequest.executeCommand(windowConfig.ip, windowConfig.port,PathConsts.prefix, PathConsts.WINDOWCONFIG, param, true).thenCompose(r -> {
                if (r.length() < 5) {
                    System.out.println(r);
                    return newFailedFuture(new RedisAuxException(r));

                }
                return CompletableFuture.completedFuture(null);
            });
        } catch (Exception e) {
            System.out.println("send async fail cause:" + e.getMessage());
        }
        return Result.success(windowConfig.groupId);
    }
    @PostMapping("/sendCommond/getGroupIds")
    public Result<String> getGroupIds(@RequestBody BaseVO baseVO) throws ExecutionException, InterruptedException {
        CompletableFuture<String> stringCompletableFuture = sendRequest.executeCommand(baseVO.ip, baseVO.port, PathConsts.prefix,PathConsts.GETGROUPIDS, null, false);
        return Result.success(stringCompletableFuture.get());
    }
    @PostMapping("/sendCommond/getCount")
    public Result<String> getCount(@RequestBody BaseVO baseVO) throws ExecutionException, InterruptedException {
        Map<String,String> map=new HashMap<>();
        map.put("groupId", baseVO.groupId);
        CompletableFuture<String> stringCompletableFuture = sendRequest.executeCommand(baseVO.ip, baseVO.port,PathConsts.prefix, PathConsts.GETCOUNT, map, false);
        return Result.success(stringCompletableFuture.get());
    }


    private Map<String, String> changeEntity2Map(Class clazz, Object object) throws IllegalAccessException {
        Map<String, String> map = new HashMap<>();
        Field[] parentFields = clazz.getSuperclass().getFields();
        Field[] sonFields = clazz.getFields();
        for (Field parentField : parentFields) {
            String name = parentField.getName();
            map.put(name, parentField.get(object).toString());
        }
        for (Field sonField : sonFields) {
            String name = sonField.getName();
            map.put(name, sonField.get(object).toString());
        }
        return map;
    }

    public static <R> CompletableFuture<R> newFailedFuture(Throwable ex) {
        CompletableFuture<R> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

}
*/
