package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.remote.protocol.ReturnT;
import com.xxl.job.remote.protocol.request.TriggerParam;

import java.util.List;

/**
 * Created by xuxueli on 17/3/10.
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, String appname, List<String> addressList){
        return new ReturnT<String>(addressList.get(0));
    }

}
