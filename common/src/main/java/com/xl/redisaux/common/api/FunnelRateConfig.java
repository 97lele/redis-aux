package com.xl.redisaux.common.api;

import com.xl.redisaux.common.consts.LimiterConstants;
import com.xl.redisaux.common.enums.TimeUnitEnum;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/16 15:48
 */
public class FunnelRateConfig {

    private Integer type = LimiterConstants.FUNNEL_LIMITER;
    //漏斗配置
    private Double capacity;

    /**
     * 每秒漏出的速率,默认单位秒
     *
     * @return
     */
    private Double funnelRate;
    /**
     * 每次请求所需加的水量,默认1
     *
     * @return
     */
    private Double requestNeed;

    private TimeUnit funnelRateUnit;

    private int unitMode;

    public FunnelRateConfig() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunnelRateConfig that = (FunnelRateConfig) o;
        return Objects.equals(type, that.type) &&
                Objects.equals(capacity, that.capacity) &&
                Objects.equals(funnelRate, that.funnelRate) &&
                Objects.equals(requestNeed, that.requestNeed) &&
                funnelRateUnit == that.funnelRateUnit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, capacity, funnelRate, requestNeed, funnelRateUnit);
    }

    public FunnelRateConfig(Builder builder) {
        this.capacity = builder.capacity;
        this.funnelRate = builder.funnelRate;
        this.requestNeed = builder.requestNeed == null ? 1 : builder.requestNeed;
        this.funnelRateUnit = builder.funnelRateUnit == null ? TimeUnit.SECONDS : builder.funnelRateUnit;
    }

    public Double getCapacity() {
        return capacity;
    }

    public void setCapacity(Double capacity) {
        this.capacity = capacity;
    }

    public Integer getType() {
        return type;
    }

    public Double getFunnelRate() {
        return funnelRate;
    }

    public void setFunnelRate(Double funnelRate) {
        this.funnelRate = funnelRate;
    }

    public Double getRequestNeed() {
        return requestNeed;
    }

    public void setRequestNeed(Double requestNeed) {
        this.requestNeed = requestNeed;
    }

    public TimeUnit getFunnelRateUnit() {
        if (funnelRateUnit == null) {
            funnelRateUnit = TimeUnitEnum.getTimeUnit(unitMode);
        }
        return funnelRateUnit;
    }

    public void setUnitMode(int unitMode) {
        if (funnelRateUnit == null) {
            funnelRateUnit = TimeUnitEnum.getTimeUnit(unitMode);
        }
        this.unitMode = unitMode;
    }

    public void setFunnelRateUnit(TimeUnit funnelRateUnit) {
        this.funnelRateUnit = funnelRateUnit;
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
        private Double funnelRate;
        /**
         * 每次请求所需要的令牌数,默认1
         *
         * @return
         */
        private Double requestNeed;

        private TimeUnit funnelRateUnit;

        public Builder capacity(Double capacity) {
            this.capacity = capacity;
            return this;
        }

        public Builder funnelRate(Double funnelRate) {
            this.funnelRate = funnelRate;
            return this;
        }

        public Builder requestNeed(Double requestNeed) {
            this.requestNeed = requestNeed;
            return this;
        }

        public Builder funnelRateUnit(TimeUnit funnelRateUnit) {
            this.funnelRateUnit = funnelRateUnit;
            return this;
        }

        public FunnelRateConfig build() {
            return new FunnelRateConfig(this);
        }
    }
}
