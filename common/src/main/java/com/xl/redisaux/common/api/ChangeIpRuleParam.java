package com.xl.redisaux.common.api;

import lombok.Data;

@Data
public class ChangeIpRuleParam extends BaseParam {
    private String rule;
    private Boolean enable;
    private Boolean white;
}
