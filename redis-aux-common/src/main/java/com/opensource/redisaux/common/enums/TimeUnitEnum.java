package com.opensource.redisaux.common.enums;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/16 22:45
 */
public enum  TimeUnitEnum {
    DAYS(TimeUnit.DAYS,4),
    HOURS(TimeUnit.HOURS,3),
    MINUTES(TimeUnit.MINUTES,2),
    SECOND(TimeUnit.SECONDS,1),
    MICROSECONDS(TimeUnit.MICROSECONDS,0);
    private TimeUnit timeUnit;
    private Integer mode;
    TimeUnitEnum(TimeUnit timeUnit,Integer mode){
        this.mode=mode;
        this.timeUnit=timeUnit;
    }

    public static Integer getMode(TimeUnit unit){
        for (TimeUnitEnum value : values()) {
            if(value.timeUnit.equals(unit)){
                return value.mode;
            }
        }
        return -1;
    }
    public static TimeUnit getTimeUnit(Integer mode){
        for (TimeUnitEnum value : values()) {
            if(value.mode.equals(mode)){
                return value.timeUnit;
            }
        }
        return TimeUnit.SECONDS;
    }
}
