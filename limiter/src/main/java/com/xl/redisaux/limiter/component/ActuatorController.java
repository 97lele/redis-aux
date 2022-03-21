package com.xl.redisaux.limiter.component;

import com.xl.redisaux.common.api.FunnelRateConfig;
import com.xl.redisaux.common.api.LimitGroupConfig;
import com.xl.redisaux.common.api.TokenRateConfig;
import com.xl.redisaux.common.api.WindowRateConfig;
import com.xl.redisaux.common.utils.IpCheckUtil;
import com.xl.redisaux.common.enums.TimeUnitEnum;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * @author lulu
 * @Date 2020/2/16 19:38
 * 可以依附应用进行本地变更，也可以通过控制台变更，控制台开启后，不启动
 */
@RestController
public class ActuatorController {

    @Resource
    private LimiterGroupService limiterGroupService;

    @GetMapping("/redis-aux/getIp")
    public String getIp(HttpServletRequest request) {
        return IpCheckUtil.getIpAddr(request);
    }

    //更改ip规则
    @PostMapping("/redis-aux/changeIpRule")
    public LimitGroupConfig changeRule(@RequestParam("groupId") String groupId,
                                       @RequestParam(value = "rule", required = false) String rule,
                                       @RequestParam(value = "enable", required = false) Boolean enable,
                                       @RequestParam(value = "white", required = false) Boolean white) {
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (white) {
            limiter.setWhiteRule(rule);
            limiter.setEnableWhiteList(enable);
        } else {
            limiter.setBlackRule(rule);
            limiter.setEnableBlackList(enable);
        }
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }

    //更改url匹配规则
    @PostMapping("/redis-aux/changeUrlRule")
    public LimitGroupConfig changeUrlRule(@RequestParam("groupId") String groupId,
                                          @RequestParam("enableUrl") String enableUrl,
                                          @RequestParam("unableUrl") String unableUrl
    ) {
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        boolean change = false;
        if (enableUrl != null) {
            change = true;
            limiter.setEnableURLPrefix(enableUrl);
        }
        if (unableUrl != null) {
            change = true;
            limiter.setUnableURLPrefix(unableUrl);
        }
        if (change) {
            limiterGroupService.save(limiter, true, false);
        }
        return limiter;
    }


    //更改模式
    @PostMapping("/redis-aux/changeLimitMode")
    public LimitGroupConfig changeMode(@RequestParam("groupId") String groupId, @RequestParam("mode") Integer mode
            , @RequestParam("removeOther") Boolean removeOther
    ) {
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (mode < 4 && mode > 0 && limiter.setCurrentMode(mode)) {
            limiterGroupService.save(limiter, true, removeOther);
        }
        return limiter;
    }

    //更改限流规则
    @PostMapping("/redis-aux/changeFunnelConfig")
    public FunnelRateConfig changeFunnelConfig(@RequestParam("groupId") String groupId,
                                               @RequestParam(value = "requestNeed", required = false) Double requestNeed,
                                               @RequestParam("capacity") Double capacity,
                                               @RequestParam("funnelRate") Double funnelRate,
                                               @RequestParam(value = "funnelRateUnit", required = false) Integer funnelRateUnit
    ) {

        FunnelRateConfig config = FunnelRateConfig.of().capacity(capacity)
                .funnelRate(funnelRate).requestNeed(requestNeed)
                .funnelRateUnit(TimeUnitEnum.getTimeUnit(funnelRateUnit)).build();
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (limiter.setFunnelRateConfig(config)) {
            limiterGroupService.save(limiter, true, false);
        }
        return limiter.getFunnelRateConfig();
    }

    @PostMapping("/redis-aux/changeWindowConfig")
    public WindowRateConfig changeWindowConfig(@RequestParam("groupId") String groupId,
                                               @RequestParam("passCount") Long passCount,
                                               @RequestParam(value = "during", required = false) Long during,
                                               @RequestParam(value = "duringUnit", required = false) Integer mode
    ) {
        WindowRateConfig config = WindowRateConfig.of().passCount(passCount).during(during).duringUnit(TimeUnitEnum.getTimeUnit(mode)).build();
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (limiter.setWindowRateConfig(config)) {
            limiterGroupService.save(limiter, true, false);
        }
        return limiter.getWindowRateConfig();
    }

    @PostMapping("/redis-aux/changeTokenConfig")
    public TokenRateConfig changeWindowConfig(@RequestParam("groupId") String groupId,
                                              @RequestParam("capacity") Double capacity,
                                              @RequestParam(value = "initToken", required = false) Double initToken,
                                              @RequestParam("tokenRate") Double tokenRate,
                                              @RequestParam(value = "requestNeed", required = false) Double requestNeed,
                                              @RequestParam(value = "duringUnit", required = false) Integer duringUnit
    ) {
        TokenRateConfig config = TokenRateConfig.of().capacity(capacity).initToken(initToken).tokenRate(tokenRate)
                .requestNeed(requestNeed).tokenRateUnit(TimeUnitEnum.getTimeUnit(duringUnit)).build();
        LimitGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (limiter.setTokenRateConfig(config)) {
            limiterGroupService.save(limiter, true, false);
        }
        return limiter.getTokenRateConfig();
    }

    @GetMapping("/redis-aux/getCount")
    public Map<String, Object> changeCountConfig(@RequestParam("groupId") String groupId
    ) {
        return limiterGroupService.getCount(groupId);
    }

    @GetMapping("/redis-aux/getGroupIds")
    public Set<String> getGroupIds() {
        return limiterGroupService.getGroupIds();
    }

}
