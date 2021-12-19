package com.xxl.job.admin.config;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.SpawnProtocol;
import akka.actor.typed.javadsl.Behaviors;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActorSystemConfig {

    @Bean(destroyMethod = "terminate")
    public ActorSystem<SpawnProtocol.Command> actorSystem() {
        Config config = ConfigFactory.load("xxl-job.conf");
        ActorSystem<SpawnProtocol.Command> actorSystem = ActorSystem.create(Behaviors.setup(ctx -> SpawnProtocol.create()), "xxl-job", config);
        actorSystem.whenTerminated().onComplete(done -> {
            if (done.isSuccess()) {
                actorSystem.log().info("ActorSystem terminate success!");
            } else {
                actorSystem.log().error("ActorSystem terminate failure!", done.failed().get());
            }
            return done.get();
        }, actorSystem.executionContext());
        return actorSystem;
    }

}