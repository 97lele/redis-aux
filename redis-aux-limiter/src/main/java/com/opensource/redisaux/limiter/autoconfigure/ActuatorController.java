package com.opensource.redisaux.limiter.autoconfigure;

import com.opensource.redisaux.common.IpCheckUtil;
import com.opensource.redisaux.common.TimeUnitEnum;
import com.opensource.redisaux.limiter.core.group.config.FunnelRateConfig;
import com.opensource.redisaux.limiter.core.group.config.LimiteGroupConfig;
import com.opensource.redisaux.limiter.core.group.config.TokenRateConfig;
import com.opensource.redisaux.limiter.core.group.config.WindowRateConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
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

    @GetMapping("/getIp")
    public String getIp(HttpServletRequest request) {
        return IpCheckUtil.getIpAddr(request);
    }

    //更改ip规则
    @PostMapping("/changeIpRule")
    public LimiteGroupConfig changeRule(@RequestParam("groupId") String groupId,
                                        @RequestParam("rule") String rule,
                                        @RequestParam("enable") Boolean enable,
                                        @RequestParam("white") Boolean white) {
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


    //更改模式
    @PostMapping("/changeLimitMode")
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
    @PostMapping("/changeFunnelConfig")
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
        limiter.setFunnelConfig(config);
        limiterGroupService.save(limiter, true, false);
        return limiter;
    }

    @PostMapping("/changeWindowConfig")
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

    @PostMapping("/changeTokenConfig")
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

    @GetMapping("/getCount/{groupId}")
    public Map<String, String> changeCountConfig(@PathVariable("groupId") String groupId
    ) {
        return limiterGroupService.getCount(groupId);
    }

}
