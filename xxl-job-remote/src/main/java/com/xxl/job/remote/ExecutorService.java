package com.xxl.job.remote;

import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;

import java.util.concurrent.CompletableFuture;

public interface ExecutorService {

    CompletableFuture<ReturnT<String>> beat();

    CompletableFuture<ReturnT<String>> idleBeat(IdleBeatParam idleBeatParam);

    CompletableFuture<ReturnT<String>> run(TriggerParam triggerParam);

    CompletableFuture<ReturnT<String>> kill(KillParam killParam);

    CompletableFuture<ReturnT<LogResult>> log(LogParam logParam);
}
