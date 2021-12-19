package com.xxl.job.admin.core.alarm;

import com.xxl.job.dao.model.XxlJobInfo;
import com.xxl.job.dao.model.XxlJobLog;

public interface JobAlarm {

    boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
