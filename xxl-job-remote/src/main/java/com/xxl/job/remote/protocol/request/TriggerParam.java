package com.xxl.job.remote.protocol.request;

import com.xxl.job.remote.protocol.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerParam extends Request {

    private static final long serialVersionUID = -3902402878991252002L;

    private int jobId;

    private String executorHandler;
    private String executorParams;
    private String executorBlockStrategy;
    private int executorTimeout;

    private long logId;
    private long logDateTime;

    private String glueType;
    private String glueSource;
    private long glueUpdatetime;

    private int broadcastIndex;
    private int broadcastTotal;
}
