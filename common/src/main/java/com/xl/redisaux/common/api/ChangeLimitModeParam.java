package com.xl.redisaux.common.api;

import lombok.Data;

@Data
public class ChangeLimitModeParam extends BaseParam{
    private String groupId;
    private Boolean removeOther;
}
