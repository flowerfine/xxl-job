package com.xxl.job.core.remote.client;

import akka.actor.ActorSelection;
import akka.actor.Address;
import akka.actor.AddressFromURIString;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.PreRestart;
import akka.actor.typed.javadsl.*;
import akka.actor.typed.receptionist.Receptionist;
import akka.actor.typed.receptionist.ServiceKey;
import com.xxl.job.core.server.AkkaServer;
import com.xxl.job.core.server.ServiceKeyHelper;
import com.xxl.job.remote.ActorSelectionHelper;

import java.util.Optional;
import java.util.Set;

public class ExecutorBehavior extends AbstractBehavior<ExecutorBehavior.Command> {

    private String appname;
    private String address;

    private ServiceKey<AkkaServer.Command> serviceKey;
    private Set<ActorRef<AkkaServer.Command>> executors;

    public ExecutorBehavior(ActorContext<Command> context, String appname, String address) {
        super(context);
        this.appname = appname;
        this.address = address;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onSignal(PreRestart.class, this::onStart)
                .onSignal(PostStop.class, this::onStop)
                .onMessage(ListingWrapper.class, this::onListing)
                .onMessage(RemoteActorCommand.class, this::onRemote)
                .build();
    }

    private Behavior<Command> onListing(ListingWrapper listingWrapper) {
        this.executors = listingWrapper.listing.getServiceInstances(serviceKey);
        this.executors.forEach(executor -> System.out.println("执行器地址: " + executor.path()));
        return Behaviors.same();
    }

    private Behavior<Command> onRemote(RemoteActorCommand command) {
        Optional<ActorRef<AkkaServer.Command>> optional = executors.stream().findAny();
        if (optional.isPresent()) {
            command.replyTo.tell(optional.get());
        } else {
            command.replyTo.tell(null);
        }
        return Behaviors.stopped();
    }

    private Behavior<Command> onStart(PreRestart restart) {
        this.serviceKey = ServiceKeyHelper.getServiceKey(AkkaServer.Command.class, appname);

        String canonicalAddress = ActorSelectionHelper.getAppnameAddress(appname, this.address);
        Address address = AddressFromURIString.parse(canonicalAddress);
        String receptionistPath = getContext().getSystem().receptionist().path().toStringWithAddress(address);
        ActorSelection actorSelection = getContext().classicActorContext().actorSelection(receptionistPath);
        ActorRef<Receptionist.Listing> listingActorRef = getContext().messageAdapter(Receptionist.Listing.class, ListingWrapper::new);

        Receptionist.Command command = Receptionist.find(serviceKey, listingActorRef);
        actorSelection.tell(command, Adapter.toClassic(getContext().getSelf()));
        return Behaviors.same();
    }

    private Behavior<Command> onStop(PostStop stop) {
        return Behaviors.stopped();
    }

    public interface Command {

    }

    public static class ListingWrapper implements Command {
        public final Receptionist.Listing listing;

        public ListingWrapper(Receptionist.Listing listing) {
            this.listing = listing;
        }
    }

    public static class RemoteActorCommand implements Command {
        public final ActorRef<ActorRef<AkkaServer.Command>> replyTo;
        public RemoteActorCommand(ActorRef<ActorRef<AkkaServer.Command>> replyTo) {
            this.replyTo = replyTo;
        }
    }
}
