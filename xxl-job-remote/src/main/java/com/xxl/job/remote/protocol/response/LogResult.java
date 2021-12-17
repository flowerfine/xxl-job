package com.xxl.job.remote.protocol.response;

import com.xxl.job.remote.protocol.Response;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogResult extends Response {

    private static final long serialVersionUID = 2007656053246044538L;

    private int fromLineNum;
    private int toLineNum;
    private String logContent;
    private boolean isEnd;
}
