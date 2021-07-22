package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.common.api.ChangeLimitModeParam;
import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.transport.common.SupportAction;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * @author tanjl11
 * @date 2021/07/21 10:00
 */
@Service
public class InstanceManagerService {
    @Resource
    private InstanceInfoPuller puller;

    public Set<InstanceInfo> getInstanceInfo() {
        Set<InstanceInfo> instanceInfos = puller.getConfigMap().keySet();
        return instanceInfos;
    }

    public List<LimitGroupConfig> getConfigByInstanceInfo(String uniqueKey) {
        InstanceInfo instanceInfo = InstanceInfo.uniqueKey2Instance(uniqueKey);
        return puller.getByInstanceInfo(instanceInfo);
    }

    public LimitGroupConfig changeConfig(String uniqueKey, ChangeLimitModeParam changeLimitModeParam) {
        LimitGroupConfig limitGroupConfig = InstanceInfoPuller.performRequest(uniqueKey, LimitGroupConfig.class, SupportAction.CHANGE_LIMIT_MODE, changeLimitModeParam);
        return limitGroupConfig;
    }

}
