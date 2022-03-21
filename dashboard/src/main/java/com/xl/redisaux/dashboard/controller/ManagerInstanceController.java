package com.xl.redisaux.dashboard.controller;

import com.xl.redisaux.common.api.*;
import com.xl.redisaux.dashboard.service.InstanceManagerService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Set;

/**
 * @author tanjl11
 * @date 2021/07/20 16:17
 */
@RestController
@RequestMapping("/instance")
public class ManagerInstanceController {

    @Resource
    private InstanceManagerService managerService;

    @GetMapping("/getAllInstance")
    public Set<InstanceInfo> getAllInstance() {
        return managerService.getInstanceInfo();
    }

    @GetMapping("/getConfig")
    public Collection<LimitGroupConfig> getConfigByInstanceInfo(String uniqueKey) {
        return managerService.getConfigByInstanceInfo(uniqueKey);
    }

    @PostMapping("/changeMode")
    public Integer changeConfig(@RequestBody ChangeLimitModeParam param) {
        return managerService.changeMode(param);
    }


    @PostMapping("/changeUrlRule")
    public String changeUrlRule(@RequestBody ChangeUrlRuleParam urlRuleParam) {
        return managerService.changeUrlRule(urlRuleParam);
    }

    @PostMapping("/tokenChange")
    public TokenRateConfig tokenChange(@RequestBody TokenChangeParam tokenChangeParam) {
        return managerService.tokenChange(tokenChangeParam);
    }

    @PostMapping("/windowChange")
    public WindowRateConfig windowChange(@RequestBody WindowChangeParam changeParam) {
        return managerService.windowChange(changeParam);
    }

    @PostMapping("/funnelChange")
    public FunnelRateConfig windowChange(@RequestBody FunnelChangeParam changeParam) {
        return managerService.funnelChange(changeParam);
    }

    @PostMapping("/changeBWRule")
    public String changeIpRule(@RequestBody ChangeIpRuleParam param) {
        return managerService.changeBWRule(param);
    }
}
