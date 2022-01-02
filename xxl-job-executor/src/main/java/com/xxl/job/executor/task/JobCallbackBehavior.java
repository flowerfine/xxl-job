package com.xxl.job.executor.task;

import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Receive;

import java.io.Serializable;

/**
 * todo 任务处理的回调，添加批量处理
 */
public class JobCallbackBehavior extends AbstractBehavior<JobCallbackBehavior.Command> {

    public JobCallbackBehavior(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return null;
    }

    public interface Command extends Serializable {

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
