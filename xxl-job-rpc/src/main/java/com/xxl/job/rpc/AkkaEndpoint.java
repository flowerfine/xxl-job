package com.xxl.job.rpc;

import akka.actor.Address;
import akka.actor.typed.ActorRef;
import com.xxl.job.rpc.message.Message;

import java.util.concurrent.CompletableFuture;

public class AkkaEndpoint implements RpcServer {

    private final String host;
    private final int port;
    private final ActorRef<Message> actorRef;

    public AkkaEndpoint(ActorRef<Message> actorRef) {
        this.actorRef = actorRef;
        final Address address = actorRef.path().address();
        this.host = address.getHost().orElse("localhost");
        this.port = address.getPort().orElse(-1);
    }

    @Override
    public ActorRef<Message> getActorRef() {
        return actorRef;
    }

    @Override
    public CompletableFuture<Void> getTerminatedFuture() {
        return new CompletableFuture<>();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }
}
