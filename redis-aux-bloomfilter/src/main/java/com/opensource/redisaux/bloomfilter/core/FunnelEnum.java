package com.opensource.redisaux.bloomfilter.core;

import com.google.common.base.Charsets;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;

/**
 * @author: lele
 * @date: 2019/12/20 下午6:19
 * 不同的类对应不同的funnel,用于hashobject
 */
public enum FunnelEnum {
    STRINGFUNNEL(Funnels.stringFunnel(Charsets.UTF_8), String.class),
    INTFUNNEL(Funnels.integerFunnel(), Integer.class),
    LONGFUNNEL(Funnels.longFunnel(), Long.class),
    BYTEFUNNEL(Funnels.byteArrayFunnel(), Byte.class),
    ;

    private Funnel funnel;
    private Class code;

    FunnelEnum(Funnel funnels, Class code) {
        this.code = code;
        this.funnel = funnels;
    }

    public Funnel getFunnel() {
        return funnel;
    }

    public Class getCode() {
        return code;
    }


}