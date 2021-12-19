package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.thread.JobCompleteHelper;
import com.xxl.job.admin.core.thread.JobRegistryHelper;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.HandleCallbackParam;
import com.xxl.job.remote.protocol.request.RegistryParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminBizImpl implements AdminBiz {

    private JobCompleteHelper jobCompleteHelper;
    private JobRegistryHelper jobRegistryHelper;

    public AdminBizImpl(JobCompleteHelper jobCompleteHelper, JobRegistryHelper jobRegistryHelper) {
        this.jobCompleteHelper = jobCompleteHelper;
        this.jobRegistryHelper = jobRegistryHelper;
    }

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return jobCompleteHelper.callback(callbackParamList);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return jobRegistryHelper.registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return jobRegistryHelper.registryRemove(registryParam);
    }

}
