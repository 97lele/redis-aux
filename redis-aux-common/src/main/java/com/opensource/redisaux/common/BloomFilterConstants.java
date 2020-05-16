package com.opensource.redisaux.common;

/**
 * @author: lele
 * @date: 2020/1/2 上午10:04
 */
public final class BloomFilterConstants {

    private BloomFilterConstants() {

    }

    public static final Long TRUE = 1L;
    public static final String LAMBDAMETHODNAME = "writeReplace";
    public static final String GET = "get";
    public static final String IS = "is";
    public static final String SCAPATH = "bloomFilterPath";
    public static final long MAX_REDIS_BIT_SIZE = 4294967296L;
    public static final String PATH = "com.opensource.redisaux.bloomfilter.autoconfigure";
    public static final String INNERTEMPLATE = "bloom";
    public static final long CHECK_TASK_PER_SECOND = 5L;
    public static final int SINGLE=0;
    public static final int COPY=1;
    public static final int NEW=2;


}