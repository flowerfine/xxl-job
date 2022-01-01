package com.xxl.job.rpc;

import akka.actor.typed.ActorRef;
import com.xxl.job.rpc.message.Message;

import java.util.concurrent.CompletableFuture;

public interface RpcServer extends RpcGateway {

    ActorRef<Message> getActorRef();

    CompletableFuture<Void> getTerminatedFuture();
}
