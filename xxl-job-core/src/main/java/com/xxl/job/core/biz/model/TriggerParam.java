package com.xxl.job.core.biz.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class TriggerParam implements Serializable {

    private static final long serialVersionUID = 42L;

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
