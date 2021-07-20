package com.xl.redisaux.common.enums;

import lombok.Getter;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/16 22:45
 */
@Getter
public enum TimeUnitEnum {
    DAYS(TimeUnit.DAYS, 3),
    HOURS(TimeUnit.HOURS, 2),
    MINUTES(TimeUnit.MINUTES, 1),
    SECOND(TimeUnit.SECONDS, 0),
    MILLISECONDS(TimeUnit.MILLISECONDS, 4),
    MICROSECONDS(TimeUnit.MICROSECONDS, 5);

    private TimeUnit timeUnit;
    private Integer mode;

    TimeUnitEnum(TimeUnit timeUnit, Integer mode) {
        this.mode = mode;
        this.timeUnit = timeUnit;
    }

    public static Integer getMode(TimeUnit unit) {
        for (TimeUnitEnum value : values()) {
            if (value.timeUnit.equals(unit)) {
                return value.mode;
            }
        }
        return -1;
    }

    public static TimeUnit getTimeUnit(Integer mode) {
        for (TimeUnitEnum value : values()) {
            if (value.mode.equals(mode)) {
                return value.timeUnit;
            }
        }
        return TimeUnit.SECONDS;
    }
}
