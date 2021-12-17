package com.xxl.job.remote.protocol.request;

import com.xxl.job.remote.protocol.Request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogParam extends Request {

    private static final long serialVersionUID = 5414822147140481410L;

    private long logId;
    private long logDateTim;
    private int fromLineNum;
}