package com.xl.redisaux.common.api;

import lombok.Data;

@Data
public class ChangeUrlRuleParam extends BaseParam{
    private String enableUrl;
    private String unableUrl;
}
