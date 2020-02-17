package com.opensource.redisaux.limiter.core.group.config;

import com.opensource.redisaux.common.CommonUtil;
import com.opensource.redisaux.common.LimiterConstants;

import java.util.Collections;
import java.util.List;

/**
 * @author lulu
 * @Date 2020/2/15 16:16
 */

public class LimiteGroupConfig {
    public LimiteGroupConfig() {

    }

    public LimiteGroupConfig(LimiteGroupConfig.Builder builder) {
        this.remark = builder.remark;
        this.id = builder.id;
        this.blackRule = builder.blackRule;
        this.whiteRule = builder.whiteRule;
        this.currentMode = builder.currentMode;
        this.funnelRateConfig = builder.funnelRateConfig;
        this.windowRateConfig = builder.windowRateConfig;
        this.tokenRateConfig = builder.tokenRateConfig;
        this.blackRuleFallback = builder.blackRuleFallback == null ? "" : blackRuleFallback;
        this.enableWhiteList = builder.enableWhiteList == null ? false : enableWhiteList;
        this.enableBlackList = builder.enableBlackList == null ? false : enableBlackList;
        this.enableURLPrefix = builder.enableURLPrefix == null ? "/*" : enableURLPrefix;
        this.unableURLPrefix = builder.unableURLPrefix == null ? "" : unableURLPrefix;

    }

    private String remark;

    private String id;
    //以分号分隔的形式
    private String blackRule;
    //以分号分隔的形式
    private String whiteRule;

    private volatile Integer currentMode;

    private List<String> funnelKeyName;

    private FunnelRateConfig funnelRateConfig;

    private List<String> tokenKeyName;

    private TokenRateConfig tokenRateConfig;

    private List<String> windowKeyName;

    private WindowRateConfig windowRateConfig;

    private Boolean enableBlackList;

    private Boolean enableWhiteList;

    private String blackRuleFallback;

    private String enableURLPrefix;

    private String unableURLPrefix;

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

    public String getBlackRuleFallback() {
        return blackRuleFallback;
    }

    public Boolean isEnableWhiteList() {
        return enableWhiteList;
    }

    public void setEnableWhiteList(Boolean enableWhiteList) {
        this.enableWhiteList = enableWhiteList;
    }

    public Boolean isEnableBlackList() {
        return enableBlackList;
    }

    public void setEnableBlackList(Boolean enableBlackList) {
        this.enableBlackList = enableBlackList;
    }

    public void setBlackRule(String blackRule) {
        this.blackRule = blackRule;
    }

    public void setWhiteRule(String whiteRule) {
        this.whiteRule = whiteRule;
    }

    public String getRemark() {
        return remark;
    }

    public String getId() {
        return id;
    }

    public String getBlackRule() {
        return blackRule;
    }

    public String getWhiteRule() {
        return whiteRule;
    }

    public Integer getCurrentMode() {
        return currentMode;
    }

    public FunnelRateConfig getFunnelRateConfig() {
        return funnelRateConfig;
    }

    public TokenRateConfig getTokenRateConfig() {
        return tokenRateConfig;
    }

    public WindowRateConfig getWindowRateConfig() {
        return windowRateConfig;
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

    //change ipRule
    public void changeWriteRule(String whiteRule) {
        this.whiteRule = whiteRule;
    }

    public void changeBlackRule(String blackRule) {
        this.blackRule = blackRule;
    }

    //change setCurrentMode
    public void setCurrentMode(Integer currentMode) {
        this.currentMode = currentMode;
    }
    //change config

    public void setFunnelConfig(FunnelRateConfig funnel) {
        this.funnelRateConfig = funnel;
    }

    public void setTokenRateConfig(TokenRateConfig tokenRateConfig) {
        this.tokenRateConfig = tokenRateConfig;
    }

    public void setWindowRateConfig(WindowRateConfig windowRateConfig) {
        this.windowRateConfig = windowRateConfig;
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

        public LimiteGroupConfig build() {
            return new LimiteGroupConfig(this);
        }
    }
}
