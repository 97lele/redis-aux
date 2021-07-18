package com.xl.redisaux.common.api;

import com.xl.redisaux.common.enums.TimeUnitEnum;

public class TokenChangeParam extends BaseParam {
    private Double capacity;
    private Double initToken;
    private Double tokenRate;
    private Double requestNeed;
    private Integer duringUnit;

    public TokenRateConfig toConfig(){
        return TokenRateConfig.of().capacity(capacity).initToken(initToken).tokenRate(tokenRate)
                .requestNeed(requestNeed).tokenRateUnit(TimeUnitEnum.getTimeUnit(duringUnit)).build();
    }

}
