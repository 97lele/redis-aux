package com.xl.redisaux.transport.server;


import com.xl.redisaux.transport.server.handler.SimpleHandler;
import java.util.concurrent.ExecutionException;

public class ServerInitlizerDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ServerRemoteService start = ServerRemoteService.of(8090)
                .addHandler(new SimpleHandler(8090))
                .supportHeartBeat(3, 3)
                .start();
//        start.close();
    }
}
