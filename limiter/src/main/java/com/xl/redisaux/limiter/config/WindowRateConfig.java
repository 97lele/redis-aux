package com.xl.redisaux.limiter.config;

import com.xl.redisaux.common.consts.LimiterConstants;

import java.util.concurrent.TimeUnit;

/**
 * @author lulu
 * @Date 2020/2/16 15:48
 */
public class WindowRateConfig {
    public  int type= LimiterConstants.WINDOW_LIMITER;

    private Long passCount;

    private TimeUnit duringUnit;

    private Long during;

    public WindowRateConfig() {

    }

    public WindowRateConfig(Builder builder) {
        this.during = builder.during == null ? 1L : builder.during;
        this.passCount = builder.passCount;
        this.duringUnit = builder.duringUnit == null ? TimeUnit.SECONDS : builder.duringUnit;
    }

    public Long getPassCount() {
        return passCount;
    }
    public Integer getType() {
        return type;
    }
    public void setPassCount(Long passCount) {
        this.passCount = passCount;
    }

    public TimeUnit getDuringUnit() {
        return duringUnit;
    }

    public void setDuringUnit(TimeUnit duringUnit) {
        this.duringUnit = duringUnit;
    }

    public Long getDuring() {
        return during;
    }

    public void setDuring(Long during) {
        this.during = during;
    }

    public static Builder of() {
        return new Builder();
    }

    public static class Builder {
        private Long passCount;

        /**
         * 令牌生成速率,默认秒
         *
         * @return
         */
        private TimeUnit duringUnit;
        /**
         * 每次请求所需要的令牌数,默认1
         *
         * @return
         */
        private Long during;


        public Builder passCount(Long passCount) {
            this.passCount = passCount;
            return this;
        }

        public Builder duringUnit(TimeUnit duringUnit) {
            this.duringUnit = duringUnit;
            return this;
        }

        public Builder during(Long during) {
            this.during = during;
            return this;
        }

        public WindowRateConfig build() {
            return new WindowRateConfig(this);
        }
    }
}
