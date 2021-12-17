package com.xxl.job.remote;

import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.IdleBeatParam;
import com.xxl.job.remote.protocol.request.KillParam;
import com.xxl.job.remote.protocol.request.LogParam;
import com.xxl.job.remote.protocol.request.TriggerParam;
import com.xxl.job.remote.protocol.response.LogResult;

public interface ExecutorService {

    ReturnT<String> beat();

    ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);

    ReturnT<String> run(TriggerParam triggerParam);

    ReturnT<String> kill(KillParam killParam);

    ReturnT<LogResult> log(LogParam logParam);
}
