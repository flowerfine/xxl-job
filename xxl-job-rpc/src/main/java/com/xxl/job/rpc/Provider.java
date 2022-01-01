package com.xxl.job.rpc;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.xxl.job.rpc.echo.EchoServiceImpl;
import com.xxl.job.rpc.util.AkkaUtil;

import java.util.concurrent.CompletableFuture;

public class Provider {

    public static void main(String[] args) throws Exception {
        ActorSystem<SpawnProtocol.Command> actorSystem = AkkaUtil.startProviderActorSystem();
        RpcService rpcService = new AkkaRpcService(actorSystem);
        CompletableFuture<RpcServer> future = rpcService.start("echo", new EchoServiceImpl());
        future.whenComplete((server, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
            } else {
                System.out.println("启动完毕!");
            }
        });
    }
}
