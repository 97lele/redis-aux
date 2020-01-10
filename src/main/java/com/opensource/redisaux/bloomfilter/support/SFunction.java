package com.opensource.redisaux.bloomfilter.support;

import java.io.Serializable;

@FunctionalInterface
public interface SFunction<T> extends Serializable {
    Object get(T object);
}