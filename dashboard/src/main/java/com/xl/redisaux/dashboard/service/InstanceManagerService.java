package com.xl.redisaux.dashboard.service;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.transport.common.SupportAction;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * @author tanjl11
 * @date 2021/07/21 10:00
 */
@Service
public class InstanceManagerService {
    @Resource
    private InstanceInfoPuller puller;

    /**
     * 获取所有活跃的实例信息
     *
     * @return
     */
    public Set<InstanceInfo> getInstanceInfo() {
        return puller.getInstanceInfo();
    }

    /**
     * 获取实例信息
     *
     * @param uniqueKey
     * @return
     */
    public Collection<LimitGroupConfig> getConfigByInstanceInfo(String uniqueKey) {
        InstanceInfo instanceInfo = InstanceInfo.uniqueKey2Instance(uniqueKey);
        return puller.getByInstanceInfo(instanceInfo);
    }


    /**
     * 变更限流模式
     *
     * @param changeLimitModeParam
     * @return
     */
    public Integer changeMode(ChangeLimitModeParam changeLimitModeParam) {
        Integer mode = InstanceInfoPuller.performRequest(changeLimitModeParam.getUniqueKey(), Integer.class, SupportAction.CHANGE_LIMIT_MODE, changeLimitModeParam);
        LimitGroupConfig config = getByParam(changeLimitModeParam);
        config.setCurrentMode(mode);
        InstanceInfo instanceInfo = changeLimitModeParam.toInstanceInfo();
        writeResult(instanceInfo, config);
        return mode;
    }

    /**
     * 变更匹配的url
     *
     * @param param
     * @return
     */
    public String changeUrlRule(ChangeUrlRuleParam param) {
        String res = InstanceInfoPuller.performRequest(param.getUniqueKey(), String.class, SupportAction.CHANGE_URL_RULE, param);
        LimitGroupConfig config = getByParam(param);
        String[] split = res.split("@@");
        config.setEnableURLPrefix(split[0]);
        if (split.length > 1) {
            config.setUnableURLPrefix(split[1]);
        }
        InstanceInfo instanceInfo = param.toInstanceInfo();
        writeResult(instanceInfo, config);
        return res;
    }

    /**
     * 变更ip规则
     * * @param param
     *
     * @return
     */
    public String changeIpRule(ChangeIpRuleParam param) {
        String res = InstanceInfoPuller.performRequest(param.getUniqueKey(), String.class, SupportAction.CHANGE_IP_RULE, param);
        LimitGroupConfig config = getByParam(param);
        String rule = param.getRule();
        if (param.getWhite()) {
            config.setWhiteRule(rule);
            config.setEnableWhiteList(param.getEnable());
        } else {
            config.setBlackRule(rule);
            config.setEnableBlackList(param.getEnable());
        }
        InstanceInfo instanceInfo = param.toInstanceInfo();
        writeResult(instanceInfo, config);
        return res;
    }

    /**
     * 变更令牌桶限流规则
     * * @param param
     *
     * @return
     */
    public TokenRateConfig tokenChange(TokenChangeParam param) {
        TokenRateConfig tokenRateConfig = InstanceInfoPuller.performRequest(param.getUniqueKey(), TokenRateConfig.class, SupportAction.TOKEN_CHANGE, param);
        LimitGroupConfig config = getByParam(param);
        config.setTokenRateConfig(tokenRateConfig);
        InstanceInfo instanceInfo = param.toInstanceInfo();
        writeResult(instanceInfo, config);
        return tokenRateConfig;
    }

    /**
     * 变更滑动窗口规则
     * * @param param
     *
     * @return
     */
    public WindowRateConfig windowChange(WindowChangeParam param) {
        WindowRateConfig windowRateConfig = InstanceInfoPuller.performRequest(param.getUniqueKey(), WindowRateConfig.class, SupportAction.WINDOW_CHANGE, param);
        LimitGroupConfig config = getByParam(param);
        config.setWindowRateConfig(windowRateConfig);
        InstanceInfo instanceInfo = param.toInstanceInfo();
        writeResult(instanceInfo, config);
        return windowRateConfig;
    }

    /**
     * 变更漏斗限流规则
     *
     * @param param
     * @return
     */
    public FunnelRateConfig funnelChange(FunnelChangeParam param) {
        FunnelRateConfig funnelRateConfig = InstanceInfoPuller.performRequest(param.getUniqueKey(), FunnelRateConfig.class, SupportAction.FUNNEL_CHANGE, param);
        LimitGroupConfig config = getByParam(param);
        config.setFunnelRateConfig(funnelRateConfig);
        InstanceInfo instanceInfo = param.toInstanceInfo();
        writeResult(instanceInfo, config);
        return funnelRateConfig;
    }


    private LimitGroupConfig getByParam(BaseParam param) {
        String groupId = param.getGroupId();
        String uniqueKey = param.getUniqueKey();
        InstanceInfo instanceInfo = InstanceInfo.uniqueKey2Instance(uniqueKey);
        return puller.getByParam(instanceInfo, groupId);
    }

    private void writeResult(InstanceInfo instanceInfo, LimitGroupConfig config) {
        puller.writeResult(instanceInfo, Collections.singletonList(config));
    }
}
