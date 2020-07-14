package com.xl.redisaux.bloomfilter.core.strategy;

import com.xl.redisaux.bloomfilter.core.bitarray.BitArray;
import com.google.common.hash.Funnel;

import java.util.List;

public interface Strategy {
    <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    <T> boolean mightContain(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    <T> boolean putAll(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects);

    <T> List<Boolean> mightContains(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects);

}