package com.xl.redisaux.common.exceptions;

/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 */
public class RedisAuxException extends RuntimeException {
    public RedisAuxException(String msg) {
        super(msg);
    }
}