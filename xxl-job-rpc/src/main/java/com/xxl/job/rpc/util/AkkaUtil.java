package com.xxl.job.rpc.util;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.xxl.job.rpc.ActorSelectionHelper;

public class AkkaUtil {

    private AkkaUtil() {
        throw new IllegalStateException("no instance");
    }

    public static ActorSystem<SpawnProtocol.Command> startProviderActorSystem() {
        Config config = ConfigFactory.load("provider.conf");
        return ActorSystem.create(Behaviors.setup(ctx -> SpawnProtocol.create()), ActorSelectionHelper.ACTOR_SYSTEM, config);
    }

    public static ActorSystem<SpawnProtocol.Command> startConsumerActorSystem() {
        Config config = ConfigFactory.load("consumer.conf");
        return ActorSystem.create(Behaviors.setup(ctx -> SpawnProtocol.create()), ActorSelectionHelper.ACTOR_SYSTEM, config);
    }
}
