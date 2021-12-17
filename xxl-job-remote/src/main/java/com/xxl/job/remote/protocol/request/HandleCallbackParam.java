package com.xxl.job.remote.protocol.request;

import com.xxl.job.remote.protocol.Request;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HandleCallbackParam extends Request {

    private static final long serialVersionUID = 280658338679655154L;

    private long logId;
    private long logDateTim;
    private int handleCode;
    private String handleMsg;
}
