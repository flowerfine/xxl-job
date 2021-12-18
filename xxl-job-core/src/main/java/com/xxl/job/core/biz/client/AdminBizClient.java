package com.xxl.job.core.biz.client;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.HandleCallbackParam;
import com.xxl.job.remote.protocol.request.RegistryParam;

import java.util.List;

public class AdminBizClient implements AdminBiz {

    private String addressUrl;
    private String accessToken;
    private int timeout = 3;

    public AdminBizClient(String addressUrl, String accessToken) {
        if (!addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        } else {
            this.addressUrl = addressUrl;
        }
        this.accessToken = accessToken;
    }

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return XxlJobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }

}
