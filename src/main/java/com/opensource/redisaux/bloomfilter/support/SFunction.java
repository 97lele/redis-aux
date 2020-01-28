package com.opensource.redisaux.bloomfilter.support;

import java.io.Serializable;
/**
 * @author: lele
 * @date: 2020/01/28 下午17:29
 */
@FunctionalInterface
public interface SFunction<T> extends Serializable {
    Object get(T object);
}