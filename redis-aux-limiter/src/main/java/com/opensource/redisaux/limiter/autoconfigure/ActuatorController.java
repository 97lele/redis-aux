package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.common.utils.IpCheckUtil;
import com.opensource.redisaux.common.enums.TimeUnitEnum;
import com.opensource.redisaux.limiter.core.group.config.FunnelRateConfig;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import com.opensource.redisaux.limiter.core.group.config.TokenRateConfig;
import com.opensource.redisaux.limiter.core.group.config.WindowRateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author lulu
 * @Date 2020/2/16 19:38
 */
@RestController
public class ActuatorController {

    @Autowired
    private LimiterGroupService limiterGroupService;

    @GetMapping("/redis-aux/getIp")
    public String getIp(HttpServletRequest request) {
        return IpCheckUtil.getIpAddr(request);
    }

    //更改ip规则
    @PostMapping("/redis-aux/changeIpRule")
    public LimiteGroupConfig changeRule(@RequestParam("groupId") String groupId,
                                        @RequestParam(value = "rule", required = false) String rule,
                                        @RequestParam(value = "enable", required = false) Boolean enable,
                                        @RequestParam(value = "white", required = false) Boolean white) {
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
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
    public LimiteGroupConfig changeUrlRule(@RequestParam("groupId") String groupId,
                                           @RequestParam("enableUrl") String enableUrl,
                                           @RequestParam("unableUrl") String unableUrl
                                           ) {
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (enableUrl != null) {
            limiter.setEnableURLPrefix(enableUrl);
        }
        if (unableUrl != null) {
            limiter.setUnableURLPrefix(unableUrl);
        }
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }


    //更改模式
    @PostMapping("/redis-aux/changeLimitMode")
    public LimiteGroupConfig changeMode(@RequestParam("groupId") String groupId, @RequestParam("mode") Integer mode
            , @RequestParam("removeOther") Boolean removeOther
    ) {
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        if (mode < 4 && mode > 0) {
            limiter.setCurrentMode(mode);
        }
        limiterGroupService.save(limiter, true, removeOther);
        return limiter;
    }

    //更改限流规则
    @PostMapping("/redis-aux/changeFunnelConfig")
    public LimiteGroupConfig changeFunnelConfig(@RequestParam("groupId") String groupId,
                                                @RequestParam(value = "requestNeed", required = false) Double requestNeed,
                                                @RequestParam("capacity") Double capacity,
                                                @RequestParam("funnelRate") Double funnelRate,
                                                @RequestParam(value = "funnelRateUnit", required = false) Integer funnelRateUnit
    ) {

        FunnelRateConfig config = FunnelRateConfig.of().capacity(capacity)
                .funnelRate(funnelRate).requestNeed(requestNeed)
                .funnelRateUnit(TimeUnitEnum.getTimeUnit(funnelRateUnit)).build();
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        limiter.setFunnelRateConfig(config);
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }

    @PostMapping("/redis-aux/changeWindowConfig")
    public LimiteGroupConfig changeWindowConfig(@RequestParam("groupId") String groupId,
                                                @RequestParam("passCount") Long passCount,
                                                @RequestParam(value = "during", required = false) Long during,
                                                @RequestParam(value = "duringUnit", required = false) Integer mode
    ) {
        WindowRateConfig config = WindowRateConfig.of().passCount(passCount).during(during).duringUnit(TimeUnitEnum.getTimeUnit(mode)).build();
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        limiter.setWindowRateConfig(config);
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }

    @PostMapping("/redis-aux/changeTokenConfig")
    public LimiteGroupConfig changeWindowConfig(@RequestParam("groupId") String groupId,
                                                @RequestParam("capacity") Double capacity,
                                                @RequestParam(value = "initToken", required = false) Double initToken,
                                                @RequestParam("tokenRate") Double tokenRate,
                                                @RequestParam(value = "requestNeed", required = false) Double requestNeed,
                                                @RequestParam(value = "duringUnit", required = false) Integer mode
    ) {
        TokenRateConfig config = TokenRateConfig.of().capacity(capacity).initToken(initToken).tokenRate(tokenRate)
                .requestNeed(requestNeed).tokenRateUnit(TimeUnitEnum.getTimeUnit(mode)).build();
        LimiteGroupConfig limiter = limiterGroupService.getLimiterConfig(groupId);
        limiter.setTokenRateConfig(config);
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }

    @GetMapping("/redis-aux/getCount/{groupId}")
    public Map<String, String> changeCountConfig(@PathVariable("groupId") String groupId
    ) {
        return limiterGroupService.getCount(groupId);
    }

}
