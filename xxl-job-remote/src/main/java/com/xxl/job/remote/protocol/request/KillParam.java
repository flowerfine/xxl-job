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
public class KillParam extends Request {

    private static final long serialVersionUID = 4964119367484607734L;

    private int jobId;
}
