package com.xxl.job.core.server;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import com.xxl.job.core.remote.ExecutorServiceImpl;
import com.xxl.job.rpc.ActorSelectionHelper;
import com.xxl.job.rpc.AkkaRpcService;
import com.xxl.job.rpc.RpcService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkkaServer {

    private final ActorSystem<SpawnProtocol.Command> actorSystem;
    private final RpcService rpcService;

    public AkkaServer(ActorSystem<SpawnProtocol.Command> actorSystem) {
        this.actorSystem = actorSystem;
        this.rpcService = new AkkaRpcService(actorSystem);
        startExecutor();
    }

    private void startExecutor() {
        rpcService.start(ActorSelectionHelper.EXECUTOR_ENDPOINT, new ExecutorServiceImpl());
    }
}
