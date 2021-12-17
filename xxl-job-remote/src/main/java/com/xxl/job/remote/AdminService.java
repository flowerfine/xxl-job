package com.xxl.job.remote;

import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.HandleCallbackParam;
import com.xxl.job.remote.protocol.request.RegistryParam;

import java.util.List;

public interface AdminService {

    ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    ReturnT<String> registry(RegistryParam registryParam);

    ReturnT<String> registryRemove(RegistryParam registryParam);
}
