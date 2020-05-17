package com.opensource.redisaux.bloomfilter.core.bitarray;


import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author lulu
 * @Date 2020/5/16 12:03
 * 通过cas
 */
public class LocalBitArray implements BitArray {

    private static final int LONG_ADDRESSABLE_BITS = 6;
    private AtomicLongArray data;
    private LongAdder bitCount;
    private final long bitSize;

    public LocalBitArray(Long bitSize) {
        long[] longs = new long[Ints.checkedCast(LongMath.divide(bitSize, 64, RoundingMode.CEILING))];
        this.data = new AtomicLongArray(longs);
        this.bitCount = new LongAdder();
        long bitCount = 0;
        this.bitCount.add(bitCount);
        this.bitSize = bitSize;
    }


    @Override
    public void setBitSize(long bitSize) {
        return;
    }

    @Override
    public boolean set(long[] indexs) {
        for (long bitIndex : indexs) {
            if (!getBitIndex(bitIndex, data) && setBitIndex(bitIndex, data)) {
                bitCount.increment();
            }
        }
        return true;
    }


    @Override
    public boolean get(long[] indexs) {

        for (long index : indexs) {
            if (!getBitIndex(index, data)) {
                return false;
            }
        }
        return true;
    }

    private boolean getBitIndex(long bitIndex, AtomicLongArray data) {
        return (data.get((int) (bitIndex >>> LONG_ADDRESSABLE_BITS)) & (1L << bitIndex)) != 0;
    }

    private boolean setBitIndex(long bitIndex, AtomicLongArray data) {
        int longIndex = (int) (bitIndex >>> LONG_ADDRESSABLE_BITS);
        // only cares about low 6 bits of bitIndex
        long mask = 1L << bitIndex;
        long oldValue;
        long newValue;
        do {
            oldValue = data.get(longIndex);
            newValue = oldValue | mask;
            if (oldValue == newValue) {
                return false;
            }
        } while (!data.compareAndSet(longIndex, oldValue, newValue));
        return true;
    }

    @Override
    public long bitSize() {
        return (long) data.length() * Long.SIZE;
    }

    @Override
    public void reset() {
        long[] longs = new long[Ints.checkedCast(LongMath.divide(bitSize, 64, RoundingMode.CEILING))];
        this.bitCount = new LongAdder();
        this.data = new AtomicLongArray(longs);
    }

    @Override
    public void clear() {
        this.data = null;
        this.bitCount = null;
    }

    @Override
    public List<String> getKeyList() {
        return null;
    }

    @Override
    public List<Boolean> getBatch(List indexs) {
        List<Boolean> list = new ArrayList(indexs.size());
        for (Object o : indexs) {
            long[] temp = (long[]) o;
            boolean b = get(temp);
            list.add(b);
        }
        return list;
    }

    @Override
    public boolean setBatch(List indexs) {
        for (Object o : indexs) {
            long[] temp = (long[]) o;
            set(temp);
        }
        return true;
    }



}
