package com.xl.redisaux.common.api;

import com.xl.redisaux.common.enums.TimeUnitEnum;
import lombok.Data;

@Data
public class WindowChangeParam extends BaseParam {
    private Long passCount;
    private Long during;
    private Integer mode;

    public WindowRateConfig toConfig() {
        return WindowRateConfig.of().passCount(passCount).during(during).duringUnit(TimeUnitEnum.getTimeUnit(mode)).build();
    }

}
