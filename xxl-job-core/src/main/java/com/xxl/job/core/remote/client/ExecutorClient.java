package com.xxl.job.core.remote.client;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.xxl.job.core.server.AkkaServer;
import com.xxl.job.core.server.ServiceKeyHelper;
import com.xxl.job.remote.ActorSelectionHelper;
import com.xxl.job.remote.ExecutorService;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;

import java.time.Duration;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ExecutorClient extends AbstractBehavior<ExecutorClient.Command> implements ExecutorService {

    private ServiceKey<AkkaServer.Command> serviceKey;
    private HashMap<String, String> executorRouterPathMap = new HashMap<>(8);

    private String appname;

    private Set<ActorRef<AkkaServer.Command>> executors;

    public ExecutorClient(ActorContext<Command> context, String appname, String executorHost) {
        super(context);
        this.appname = appname;

        this.serviceKey = ServiceKeyHelper.getServiceKey(AkkaServer.Command.class, appname);
        executorRouterPathMap.put("beat", ActorSelectionHelper.getExecutorRouterPath(executorHost, "beat"));
        executorRouterPathMap.put("idleBeat", ActorSelectionHelper.getExecutorRouterPath(executorHost, "idleBeat"));
        executorRouterPathMap.put("run", ActorSelectionHelper.getExecutorRouterPath(executorHost, "run"));
        executorRouterPathMap.put("kill", ActorSelectionHelper.getExecutorRouterPath(executorHost, "kill"));
        executorRouterPathMap.put("log", ActorSelectionHelper.getExecutorRouterPath(executorHost, "log"));

        Address address = AddressFromURIString.parse("akka://master@127.0.0.1:25520");
        String receptionistPath = context.getSystem().receptionist().path().toStringWithAddress(address);
        ActorSelection actorSelection = context.classicActorContext().actorSelection(receptionistPath);
        ActorRef<Receptionist.Listing> listingActorRef = context.messageAdapter(Receptionist.Listing.class, ListingWrapper::new);

        Receptionist.Command command = Receptionist.find(serviceKey, listingActorRef);
        actorSelection.tell(command, Adapter.toClassic(context.getSelf()));
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ListingWrapper.class, this::onListing)
                .build();
    }

    private Behavior<Command> onListing(ListingWrapper listingWrapper) {
        this.executors = listingWrapper.listing.getServiceInstances(serviceKey);
        return Behaviors.same();
    }

    @Override
    public ReturnT<String> beat() {
        try {
            ActorRef<AkkaServer.Command> actorRef = getExecutorInstance();
            CompletableFuture<ReturnT<String>> future = new CompletableFuture<>();
            getContext().askWithStatus(ReturnT.class,
                    actorRef,
                    Duration.ofSeconds(30L),
                    reply -> new AkkaServer.BeatCommand(reply),
                    (returnT, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(returnT);
                        }
                        return null;
                    });
            return future.get();
        } catch (Exception e) {
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        try {
            ActorRef<AkkaServer.Command> actorRef = getExecutorInstance();
            CompletableFuture<ReturnT<String>> future = new CompletableFuture<>();
            getContext().askWithStatus(ReturnT.class,
                    actorRef,
                    Duration.ofSeconds(30L),
                    reply -> new AkkaServer.IdleBeatCommand(idleBeatParam, reply),
                    (returnT, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(returnT);
                        }
                        return null;
                    });
            return future.get();
        } catch (Exception e) {
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        try {
            ActorRef<AkkaServer.Command> actorRef = getExecutorInstance();
            CompletableFuture<ReturnT<String>> future = new CompletableFuture<>();
            getContext().askWithStatus(ReturnT.class,
                    actorRef,
                    Duration.ofSeconds(30L),
                    reply -> new AkkaServer.RunCommand(triggerParam, reply),
                    (returnT, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(returnT);
                        }
                        return null;
                    });
            return future.get();
        } catch (Exception e) {
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        try {
            ActorRef<AkkaServer.Command> actorRef = getExecutorInstance();
            CompletableFuture<ReturnT<String>> future = new CompletableFuture<>();
            getContext().askWithStatus(ReturnT.class,
                    actorRef,
                    Duration.ofSeconds(30L),
                    reply -> new AkkaServer.KillCommand(killParam, reply),
                    (returnT, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(returnT);
                        }
                        return null;
                    });
            return future.get();
        } catch (Exception e) {
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        try {
            ActorRef<AkkaServer.Command> actorRef = getExecutorInstance();
            CompletableFuture<ReturnT<LogResult>> future = new CompletableFuture<>();
            getContext().askWithStatus(ReturnT.class,
                    actorRef,
                    Duration.ofSeconds(30L),
                    reply -> new AkkaServer.LogCommand(logParam, reply),
                    (returnT, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(returnT);
                        }
                        return null;
                    });
            return future.get();
        } catch (Exception e) {
            return new ReturnT(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    private ActorRef<AkkaServer.Command> getExecutorInstance() {
        Optional<ActorRef<AkkaServer.Command>> optional = executors.stream().findAny();
        return optional.get();
    }

    public interface Command {

    }

    public static class ListingWrapper implements Command {
        public final Receptionist.Listing listing;

        public ListingWrapper(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }
}
