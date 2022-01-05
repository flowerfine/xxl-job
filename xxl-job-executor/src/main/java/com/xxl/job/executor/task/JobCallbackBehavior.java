package com.xxl.job.executor.task;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.remote.protocol.request.HandleCallbackParam;

import java.io.Serializable;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class JobCallbackBehavior extends AbstractBehavior<JobCallbackBehavior.Command> {

    private final AdminBizClient client;

    private Duration timeout = Duration.ofSeconds(1L);
    private List<CallbackCommand> buffers = new LinkedList<>();

    public JobCallbackBehavior(ActorContext<Command> context, AdminBizClient client) {
        super(context);
        this.client = client;
        context.setReceiveTimeout(timeout, TIMEOUT.INSTANCE);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(TIMEOUT.INSTANCE, this::onTimeout)
                .onMessage(CallbackCommand.class, this::onCallback)
                .build();
    }

    private Behavior<Command> onTimeout() {
        flush();
        getContext().setReceiveTimeout(timeout, TIMEOUT.INSTANCE);
        return Behaviors.same();
    }

    private Behavior<Command> onCallback(CallbackCommand command) {
        buffers.add(command);
        if (buffers.size() == 20) {
            flush();
        }
        return Behaviors.same();
    }

    private void flush() {
        if (buffers.isEmpty()) {
            return;
        }
        List<HandleCallbackParam> callbacks = buffers.stream()
                .map(buffer -> new HandleCallbackParam(buffer.logId, buffer.logDateTim, buffer.handleCode, buffer.handleMsg))
                .collect(Collectors.toList());
        client.callback(callbacks);
        buffers.clear();
    }

    public interface Command extends Serializable {

    }

    public enum TIMEOUT implements Command {
        INSTANCE
    }

    public static class CallbackCommand implements Command {
        private long logId;
        private long logDateTim;
        private int handleCode;
        private String handleMsg;

        public CallbackCommand(long logId, long logDateTim, int handleCode, String handleMsg) {
            this.logId = logId;
            this.logDateTim = logDateTim;
            this.handleCode = handleCode;
            this.handleMsg = handleMsg;
        }
    }

}
