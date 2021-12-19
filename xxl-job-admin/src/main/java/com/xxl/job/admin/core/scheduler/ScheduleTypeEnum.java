package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.util.I18nUtil;

public enum ScheduleTypeEnum {

    NONE(I18nUtil.getString("schedule_type_none")),
    CRON(I18nUtil.getString("schedule_type_cron")),
    FIX_RATE(I18nUtil.getString("schedule_type_fix_rate")),
    ;
    private String title;

    ScheduleTypeEnum(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem){
        for (ScheduleTypeEnum item: ScheduleTypeEnum.values()) {
            if (item.name().equals(name)) {
                return item;
            }
        }
        return defaultItem;
    }

}
