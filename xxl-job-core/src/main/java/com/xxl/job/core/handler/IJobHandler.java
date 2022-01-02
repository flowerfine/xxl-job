package com.xxl.job.core.handler;

import com.xxl.job.remote.protocol.ReturnT;

public interface IJobHandler {

	default void init() throws Exception {
		// do something
	}

	default void destroy() throws Exception {
		// do something
	}

    void execute() throws Exception;

    default ReturnT<String> kill() throws Exception {
    	return new ReturnT<>(ReturnT.FAIL_CODE, "job killed");
	}
}
