package com.xl.redisaux.dashboard.controller;

import com.xl.redisaux.transport.server.HeartBeatServerHandler;
import com.xl.redisaux.transport.server.SendRequest;
import com.xl.redisaux.transport.vo.NodeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author lulu
 * @Date 2020/7/19 14:18
 */
@RestController
public class TestController {
    @Autowired
    SendRequest sendRequest;

    @GetMapping("/getNodes")
    public Collection<NodeVO> nodeVOS() {
        return HeartBeatServerHandler.nodeVOMap.values();
    }

    @GetMapping("/sendCommand/{ip}/{port}/{path}")
    public Object send(@PathVariable("ip") String ip, @PathVariable("port") Integer port, @PathVariable("path") String path) {
        CompletableFuture<String> future = sendRequest.executeCommand(ip, port, path, null, false);
        try {
            return future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
