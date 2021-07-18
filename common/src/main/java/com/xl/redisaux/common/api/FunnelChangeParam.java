package com.xl.redisaux.common.api;

import com.xl.redisaux.common.enums.TimeUnitEnum;
import lombok.Data;

@Data
public class FunnelChangeParam extends BaseParam{
    private Double requestNeed;
    private Double capacity;
    private Double funnelRate;
    private TimeUnitEnum timeUnitEnum;

    public FunnelRateConfig toConfig(){
        return FunnelRateConfig.of().capacity(capacity)
                .funnelRate(funnelRate).requestNeed(requestNeed)
                .funnelRateUnit(timeUnitEnum.getTimeUnit()).build();
    }
}
