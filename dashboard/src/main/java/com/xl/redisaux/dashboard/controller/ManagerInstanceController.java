package com.xl.redisaux.dashboard.controller;

import com.xl.redisaux.common.api.ChangeLimitModeParam;
import com.xl.redisaux.common.api.InstanceInfo;
import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.dashboard.service.InstanceManagerService;
import lombok.Getter;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
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
    public List<LimitGroupConfig> getConfigByInstanceInfo(String uniqueKey) {
        return managerService.getConfigByInstanceInfo(uniqueKey);
    }

    @GetMapping("/changeConfig")
    public LimitGroupConfig changeConfig(@RequestParam("url") String url, @RequestBody ChangeLimitModeParam param) {
        return managerService.changeConfig(url, param);
    }
}
