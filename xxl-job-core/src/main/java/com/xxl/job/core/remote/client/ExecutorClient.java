package com.xxl.job.core.remote.client;

import com.xxl.job.remote.ExecutorService;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;

public class ExecutorClient implements ExecutorService {

    @Override
    public ReturnT<String> beat() {
        return null;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {
        return null;
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return null;
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return null;
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return null;
    }
}
