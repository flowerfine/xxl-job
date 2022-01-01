package com.xxl.job.rpc;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.xxl.job.rpc.echo.EchoService;
import com.xxl.job.rpc.util.AkkaUtil;

public class Consumer {

    public static void main(String[] args) {
        ActorSystem<SpawnProtocol.Command> actorSystem = AkkaUtil.startConsumerActorSystem();
        RpcService rpcService = new AkkaRpcService(actorSystem);
        EchoService echoService = rpcService.connect("192.168.1.104", 25520, "echo", EchoService.class);
        String echo = echoService.echo("demo-rpc-akka");
        System.out.println(echo);
    }
}
