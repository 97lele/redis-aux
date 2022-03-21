package com.xl.redisaux.common.api;

import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.enums.TimeUnitEnum;
import com.xl.redisaux.common.utils.CommonUtil;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lulu
 * @Date 2020/2/15 16:16
 */
public class LimitGroupConfig {
    public LimitGroupConfig() {

    }

    public LimitGroupConfig(Builder builder) {
        this.remark = builder.remark;
        this.id = builder.id;
        this.blackRule = builder.blackRule;
        this.whiteRule = builder.whiteRule;
        this.currentMode = new AtomicInteger(builder.currentMode);
        this.funnelRateConfig = builder.funnelRateConfig;
        this.windowRateConfig = builder.windowRateConfig;
        this.tokenRateConfig = builder.tokenRateConfig;
        this.blackRuleFallback = builder.blackRuleFallback == null ? "" : builder.blackRuleFallback;
        this.enableWhiteList = builder.enableWhiteList;
        this.enableBlackList = builder.enableBlackList;
        this.enableURLPrefix = builder.enableURLPrefix == null ? "/*" : builder.enableURLPrefix;
        this.unableURLPrefix = builder.unableURLPrefix == null ? "" : builder.unableURLPrefix;
        this.qpsCountDuring = builder.qpsCountDuring == null ? 1 : builder.qpsCountDuring;
        this.enableQpsCount = builder.enableQpsCount;
        this.qpsCountDuringUnit = builder.qpsCountDuringUnit == null ? TimeUnit.MINUTES : builder.qpsCountDuringUnit;
        this.urlFallBack = builder.urlFallBack == null ? "" : builder.urlFallBack;
        this.bucketSize = builder.bucketSize == null ? 10 : builder.bucketSize;
        this.removeOtherLimit = builder.removeOtherLimit;
        this.saveInRedis = builder.saveInRedis;
    }

    /**
     * 备注
     */
    private String remark;
    /**
     * 配置id
     */
    private String id;
    /**
     * 黑名单ip，分号分隔
     */
    private String blackRule;
    /**
     * 白名单ip,分号分隔
     */
    private String whiteRule;

    /**
     * 当前限流模式
     */
    private AtomicInteger currentMode;

    /**
     * 漏斗限流配置
     */
    private FunnelRateConfig funnelRateConfig;
    /**
     * 令牌桶限流配置
     */
    private TokenRateConfig tokenRateConfig;
    /**
     * 窗口限流配置
     */
    private WindowRateConfig windowRateConfig;

    /**
     * 是否允许黑名单
     */
    private boolean enableBlackList;
    /**
     * 是否允许白名单
     */
    private boolean enableWhiteList;

    /**
     * 黑名单回调方法
     */
    private String blackRuleFallback;
    /**
     * 允许通过的地址前缀
     */
    private String enableURLPrefix;
    /**
     * 不允许通过的地址前缀
     */
    private String unableURLPrefix;
    /**
     * 前缀不通过调用的方法
     */
    private String urlFallBack;
    /**
     * 是否允许qps统计
     */
    private boolean enableQpsCount;
    /**
     * 持续周期
     */
    private int qpsCountDuring;
    /**
     * 周期单位
     */
    private TimeUnit qpsCountDuringUnit;

    private Integer bucketSize;

    private int qpsUnitMode;

    private boolean removeOtherLimit = true;

    public boolean isRemoveOtherLimit() {
        return removeOtherLimit;
    }

    public boolean isSaveInRedis() {
        return saveInRedis;
    }

    /**
     * 是否保存到redis
     */
    private boolean saveInRedis = true;

    public void setQpsUnitMode(int unitMode) {
        qpsCountDuringUnit = TimeUnitEnum.getTimeUnit(unitMode);
        this.qpsUnitMode = unitMode;
    }

    public Integer getBucketSize() {
        return bucketSize;
    }

    public void setBucketSize(Integer bucketSize) {
        this.bucketSize = bucketSize;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBlackRule() {
        return blackRule;
    }

    public void setBlackRule(String blackRule) {
        this.blackRule = blackRule;
    }

    public String getWhiteRule() {
        return whiteRule;
    }

    public void setWhiteRule(String whiteRule) {
        this.whiteRule = whiteRule;
    }

    public Integer getCurrentMode() {
        if (currentMode != null) {
            return currentMode.intValue();
        }
        return null;
    }

    public boolean setCurrentMode(Integer currentMode) {
        if (this.currentMode == null) {
            this.currentMode = new AtomicInteger();
            this.currentMode.set(currentMode);
            return true;
        }
        boolean equals = this.currentMode.equals(currentMode);
        if (!equals) {
            this.currentMode.set(currentMode);
        }
        return !equals;
    }

    public FunnelRateConfig getFunnelRateConfig() {
        return funnelRateConfig;
    }

    public boolean setFunnelRateConfig(FunnelRateConfig funnelRateConfig) {
        boolean b = !Objects.equals(this.funnelRateConfig, funnelRateConfig);
        if (b) {
            this.funnelRateConfig = funnelRateConfig;
        }
        return b;
    }

    public TokenRateConfig getTokenRateConfig() {
        return tokenRateConfig;
    }

    public boolean setTokenRateConfig(TokenRateConfig tokenRateConfig) {
        boolean b = !Objects.equals(this.tokenRateConfig, tokenRateConfig);
        if (b) {
            this.tokenRateConfig = tokenRateConfig;
        }
        return b;
    }

    public WindowRateConfig getWindowRateConfig() {
        return windowRateConfig;
    }

    public boolean setWindowRateConfig(
            WindowRateConfig windowRateConfig) {
        boolean b = !Objects.equals(this.windowRateConfig, windowRateConfig);
        if (b) {
            this.windowRateConfig = windowRateConfig;
        }
        return b;
    }

    public boolean isEnableBlackList() {
        return enableBlackList;
    }

    public void setEnableBlackList(boolean enableBlackList) {
        this.enableBlackList = enableBlackList;
    }

    public boolean isEnableWhiteList() {
        return enableWhiteList;
    }

    public void setEnableWhiteList(boolean enableWhiteList) {
        this.enableWhiteList = enableWhiteList;
    }

    public String getBlackRuleFallback() {
        return blackRuleFallback;
    }

    public void setBlackRuleFallback(String blackRuleFallback) {
        this.blackRuleFallback = blackRuleFallback;
    }

    public String getEnableURLPrefix() {
        return enableURLPrefix;
    }

    public void setEnableURLPrefix(String enableURLPrefix) {
        this.enableURLPrefix = enableURLPrefix;
    }

    public String getUnableURLPrefix() {
        return unableURLPrefix;
    }

    public void setUnableURLPrefix(String unableURLPrefix) {
        this.unableURLPrefix = unableURLPrefix;
    }

    public String getUrlFallBack() {
        return urlFallBack;
    }

    public void setUrlFallBack(String urlFallBack) {
        this.urlFallBack = urlFallBack;
    }

    public boolean isEnableQpsCount() {
        return enableQpsCount;
    }

    public void setEnableQpsCount(boolean enableQpsCount) {
        this.enableQpsCount = enableQpsCount;
    }

    public int getQpsCountDuring() {
        return qpsCountDuring;
    }

    public void setQpsCountDuring(int qpsCountDuring) {
        this.qpsCountDuring = qpsCountDuring;
    }

    public TimeUnit getQpsCountDuringUnit() {
        if (qpsCountDuringUnit == null) {
            qpsCountDuringUnit = TimeUnitEnum.getTimeUnit(qpsUnitMode);
        }
        return qpsCountDuringUnit;
    }

    public void setQpsCountDuringUnit(TimeUnit qpsCountDuringUnit) {
        this.qpsCountDuringUnit = qpsCountDuringUnit;
    }

    public List<String> getFunnelKeyName(String methodKey) {
        return Collections.singletonList(CommonUtil.getLimiterName(id, methodKey, LimiterConstants.FUNNEL));
    }

    public List<String> getTokenKeyName(String methodKey) {
        return Collections.singletonList(CommonUtil.getLimiterName(id, methodKey, LimiterConstants.TOKEN));
    }

    public List<String> getWindowKeyName(String methodKey) {

        return Collections.singletonList(CommonUtil.getLimiterName(id, methodKey, LimiterConstants.WINDOW));
    }

    public static Builder of(String id) {
        return new Builder().id(id);
    }

    public static class Builder {
        private String id;
        private String remark;
        private String blackRule;
        private String whiteRule;
        private Integer currentMode;
        private FunnelRateConfig funnelRateConfig;
        private TokenRateConfig tokenRateConfig;
        private WindowRateConfig windowRateConfig;
        private boolean enableWhiteList;
        private boolean enableBlackList;
        private String blackRuleFallback;
        private String enableURLPrefix;
        private String unableURLPrefix;
        private boolean enableQpsCount;
        private Integer qpsCountDuring;
        private TimeUnit qpsCountDuringUnit;
        private Integer bucketSize;
        private String urlFallBack;
        //是否删除除当前模式外的限流器信息
        private boolean removeOtherLimit;
        //是否保存到redis
        private boolean saveInRedis;

        protected Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public Builder blackRule(String blackRule) {
            this.blackRule = blackRule;
            return this;
        }

        public Builder whiteRule(String whiteRule) {
            this.whiteRule = whiteRule;
            return this;
        }

        public Builder currentMode(Integer currentMode) {
            this.currentMode = currentMode;
            return this;
        }

        public Builder funnelConfig(FunnelRateConfig funnelRateConfig) {
            this.funnelRateConfig = funnelRateConfig;
            return this;
        }

        public Builder tokenConfig(TokenRateConfig tokenRateConfig) {
            this.tokenRateConfig = tokenRateConfig;
            return this;
        }

        public Builder windowConfig(WindowRateConfig windowRateConfig) {
            this.windowRateConfig = windowRateConfig;
            return this;
        }

        public Builder enableWhiteList() {
            this.enableWhiteList = true;
            return this;
        }

        public Builder urlFallBack(String urlFallBack) {
            this.urlFallBack = urlFallBack;
            return this;
        }

        public Builder enableBlackList() {
            this.enableBlackList = true;
            return this;
        }

        public Builder blackRuleFallback(String blackRuleFallback) {
            this.blackRuleFallback = blackRuleFallback;
            return this;
        }

        public Builder enableURLPrefix(String enableURLPrefix) {
            this.enableURLPrefix = enableURLPrefix;
            return this;
        }

        public Builder unableURLPrefix(String unableURLPrefix) {
            this.unableURLPrefix = unableURLPrefix;
            return this;
        }

        //开启计数，默认1分钟
        public Builder enableCount() {
            this.enableQpsCount = true;
            return this;
        }

        public Builder qpsCountDuring(Integer countDuring, TimeUnit timeUnit) {
            this.qpsCountDuring = countDuring;
            this.qpsCountDuringUnit = timeUnit;
            return this;
        }

        public Builder bucketSize(Integer bucketSize) {
            this.bucketSize = bucketSize;
            return this;
        }

        public Builder saveInRedis() {
            this.saveInRedis = true;
            return this;
        }

        public Builder removeOtherLimit() {
            this.removeOtherLimit = true;
            return this;
        }

        public LimitGroupConfig build() {
            return new LimitGroupConfig(this);
        }
    }
}
