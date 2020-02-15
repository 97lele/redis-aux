package com.opensource.redisaux.limiter.core.group;

import com.opensource.redisaux.limiter.core.BaseRateLimiter;

/**
 * @author lulu
 * @Date 2020/2/15 12:31
 * 限流组，用于动态调整，
 */

public class LimiteGroup {

    private int mode;

    private String id;

    private String application;

    private BaseRateLimiter limiter;

}
