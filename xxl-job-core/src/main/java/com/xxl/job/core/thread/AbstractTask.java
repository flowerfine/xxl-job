package com.xxl.job.core.thread;

import io.netty.util.HashedWheelTimer;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTask implements TimerTask {

    private HashedWheelTimer timer = new HashedWheelTimer(new DefaultThreadFactory(getThreadName()));
    private volatile boolean toStop = false;

    public void start() {
        timer.newTimeout(this, 100, TimeUnit.MILLISECONDS);
    }

    public void toStop() {
        toStop = true;
    }

    protected boolean isActive() {
        return toStop == false;
    }

    protected abstract String getThreadName();
}
