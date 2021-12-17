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
public class IdleBeatParam extends Request {

    private static final long serialVersionUID = 8404481844942847734L;

    private int jobId;
}