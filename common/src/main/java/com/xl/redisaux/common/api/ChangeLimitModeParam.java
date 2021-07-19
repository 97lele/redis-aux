package com.xl.redisaux.common.api;

import lombok.Data;

@Data
public class ChangeLimitModeParam extends BaseParam{
    private Boolean removeOther;
    private Integer mode;
}
