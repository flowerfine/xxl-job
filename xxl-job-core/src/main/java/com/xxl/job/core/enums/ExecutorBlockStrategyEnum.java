package com.xxl.job.core.enums;

import lombok.Getter;
import lombok.Setter;

public enum ExecutorBlockStrategyEnum {

    SERIAL_EXECUTION("Serial execution"),
    DISCARD_LATER("Discard Later"),
    COVER_EARLY("Cover Early");

    @Setter
    @Getter
    private String title;

    ExecutorBlockStrategyEnum(String title) {
        this.title = title;
    }

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
