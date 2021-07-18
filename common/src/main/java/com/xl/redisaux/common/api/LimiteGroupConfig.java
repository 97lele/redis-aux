package com.xl.redisaux.common.api;

import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.utils.CommonUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lulu
 * @Date 2020/2/15 16:16
 */

public class LimiteGroupConfig {
    public LimiteGroupConfig() {

    }

    public LimiteGroupConfig(Builder builder) {
        this.remark = builder.remark;
        this.id = builder.id;
        this.blackRule = builder.blackRule;
        this.whiteRule = builder.whiteRule;
        this.currentMode = new AtomicInteger(builder.currentMode);
        this.funnelRateConfig = builder.funnelRateConfig;
        this.windowRateConfig = builder.windowRateConfig;
        this.tokenRateConfig = builder.tokenRateConfig;
        this.blackRuleFallback = builder.blackRuleFallback == null ? "" : builder.blackRuleFallback;
        this.enableWhiteList = builder.enableWhiteList == null ? false : builder.enableWhiteList;
        this.enableBlackList = builder.enableBlackList == null ? false : builder.enableBlackList;
        this.enableURLPrefix = builder.enableURLPrefix == null ? "/*" : builder.enableURLPrefix;
        this.unableURLPrefix = builder.unableURLPrefix == null ? "" : builder.unableURLPrefix;
        this.countDuring = builder.countDuring == null ? 1 : builder.countDuring;
        this.enableQpsCount = builder.enableCount == null ? false : builder.enableCount;
        this.countDuringUnit = builder.countDuringUnit == null ? TimeUnit.MINUTES : builder.countDuringUnit;
        this.urlFallBack = builder.urlFallBack == null ? "" : builder.urlFallBack;
        this.bucketSize = builder.bucketSize == null ? 10 : builder.bucketSize;

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
    private int countDuring;
    /**
     * 周期单位
     */
    private TimeUnit countDuringUnit;

    private Integer bucketSize;

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
        if(currentMode!=null){
            return currentMode.intValue();
        }
        return null;
    }

    public boolean setCurrentMode(Integer currentMode) {
        boolean equals = this.currentMode.equals(currentMode);
        if(!equals){
            this.currentMode.set(currentMode);
        }
        return !equals;
    }

    public FunnelRateConfig getFunnelRateConfig() {
        return funnelRateConfig;
    }

    public boolean setFunnelRateConfig(FunnelRateConfig funnelRateConfig) {
        boolean b = !this.funnelRateConfig.equals(funnelRateConfig);
        if (b) {
            this.funnelRateConfig = funnelRateConfig;
        }
        return b;
    }

    public TokenRateConfig getTokenRateConfig() {
        return tokenRateConfig;
    }

    public boolean setTokenRateConfig(TokenRateConfig tokenRateConfig) {
        boolean b = !this.tokenRateConfig.equals(tokenRateConfig);
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
        boolean b = !this.windowRateConfig.equals(windowRateConfig);
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

    public int getCountDuring() {
        return countDuring;
    }

    public void setCountDuring(int countDuring) {
        this.countDuring = countDuring;
    }

    public TimeUnit getCountDuringUnit() {
        return countDuringUnit;
    }

    public void setCountDuringUnit(TimeUnit countDuringUnit) {
        this.countDuringUnit = countDuringUnit;
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


    public void destory() {
        this.funnelRateConfig = null;
        this.windowRateConfig = null;
        this.tokenRateConfig = null;
    }

    public static Builder of() {
        return new Builder();
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
        private Boolean enableWhiteList;
        private Boolean enableBlackList;
        private String blackRuleFallback;
        private String enableURLPrefix;
        private String unableURLPrefix;
        private Boolean enableCount;
        private Integer countDuring;
        private TimeUnit countDuringUnit;
        private Integer bucketSize;
        private String urlFallBack;

        public Builder id(String id) {
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

        public Builder enableWhiteList(Boolean enableWhiteList) {
            this.enableWhiteList = enableWhiteList;
            return this;
        }

        public Builder urlFallBack(String urlFallBack) {
            this.urlFallBack = urlFallBack;
            return this;
        }

        public Builder enableBlackList(Boolean enableBlackList) {
            this.enableBlackList = enableBlackList;
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

        public Builder enableCount(Boolean enableCount) {
            this.enableCount = enableCount;
            return this;
        }

        public Builder countDuring(Integer countDuring, TimeUnit timeUnit) {
            this.countDuring = countDuring;
            this.countDuringUnit = timeUnit;
            return this;
        }

        public Builder bucketSize(Integer bucketSize) {
            this.bucketSize = bucketSize;
            return this;
        }


        public LimiteGroupConfig build() {
            return new LimiteGroupConfig(this);
        }
    }
}
