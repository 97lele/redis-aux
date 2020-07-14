package com.xl.redisaux.limiter.core.group.config;

import com.xl.redisaux.common.consts.LimiterConstants;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/16 15:49
 */
public class TokenRateConfig {
    public  int type= LimiterConstants.TOKEN_LIMITER;

    private Double capacity;

    /**
     * 令牌生成速率,默认秒
     *
     * @return
     */
    private Double tokenRate;
    /**
     * 每次请求所需要的令牌数,默认1
     *
     * @return
     */
    private Double requestNeed;

    private TimeUnit tokenRateUnit;

    private Double initToken;

    public TokenRateConfig() {

    }

    public TokenRateConfig(Builder builder) {
        this.capacity = builder.capacity;
        this.tokenRate = builder.tokenRate;
        this.requestNeed = builder.requestNeed == null ? 1L : builder.requestNeed;
        this.tokenRateUnit = builder.tokenRateUnit == null ? TimeUnit.SECONDS : builder.tokenRateUnit;
        this.initToken= builder.initToken==null?0:builder.initToken;
    }
    public Integer getType() {
        return type;
    }
    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Double getTokenRate() {
        return tokenRate;
    }

    public void setTokenRate(Double tokenRate) {
        this.tokenRate = tokenRate;
    }

    public Double getRequestNeed() {
        return requestNeed;
    }

    public void setRequestNeed(Double requestNeed) {
        this.requestNeed = requestNeed;
    }

    public TimeUnit getTokenRateUnit() {
        return tokenRateUnit;
    }

    public void setTokenRateUnit(TimeUnit tokenRateUnit) {
        this.tokenRateUnit = tokenRateUnit;
    }

    public Double getInitToken() {
        return initToken;
    }

    public void setInitToken(Double initToken) {
        this.initToken = initToken;
    }

    public static Builder of() {
        return new Builder();
    }

    public static class Builder {
        private Double capacity;

        /**
         * 令牌生成速率,默认秒
         *
         * @return
         */
        private Double tokenRate;
        /**
         * 每次请求所需要的令牌数,默认1
         *
         * @return
         */
        private Double requestNeed;

        private TimeUnit tokenRateUnit;

        private Double initToken;

        public Builder capacity(Double capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder tokenRate(Double tokenRate) {
            this.tokenRate = tokenRate;
            return this;
        }

        public Builder requestNeed(Double requestNeed) {
            this.requestNeed = requestNeed;
            return this;
        }

        public Builder tokenRateUnit(TimeUnit tokenRateUnit) {
            this.tokenRateUnit = tokenRateUnit;
            return this;
        }
        public Builder initToken(Double initToken) {
            this.initToken = initToken;
            return this;
        }
        public TokenRateConfig build() {
            return new TokenRateConfig(this);
        }
    }
}
