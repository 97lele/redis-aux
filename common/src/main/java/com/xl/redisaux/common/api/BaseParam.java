package com.xl.redisaux.common.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class BaseParam {
    protected String groupId;
    //ip:port
    protected String uniqueKey;

    public InstanceInfo toInstanceInfo() {
        return InstanceInfo.uniqueKey2Instance(uniqueKey);
    }
}
