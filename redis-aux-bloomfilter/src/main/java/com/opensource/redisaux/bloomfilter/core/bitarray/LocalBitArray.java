package com.opensource.redisaux.bloomfilter.core.bitarray;


import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;
import com.opensource.redisaux.common.BloomFilterConstants;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author lulu
 * @Date 2020/5/16 12:03
 * 通过cas
 */
public class LocalBitArray implements BitArray {

    private static final int LONG_ADDRESSABLE_BITS = 6;
    private CopyOnWriteArrayList<AtomicLongArray> dataList;
    private AtomicLongArray data;
    private LongAdder bitCount;
    private final boolean enableGrow;
    private final String key;
    private final double growRate;
    private volatile int status;
    private final long bitSize;

    public LocalBitArray(Long bitSize, String key, boolean enableGrow, double growRate) {
        long[] longs = new long[Ints.checkedCast(LongMath.divide(bitSize, 64, RoundingMode.CEILING))];
        this.data = new AtomicLongArray(longs);
        this.bitCount = new LongAdder();
        long bitCount = 0;
        this.bitCount.add(bitCount);
        this.key = key;
        this.bitSize = bitSize;
        this.growRate = growRate;
        this.enableGrow = enableGrow;
    }


    @Override
    public void setBitSize(long bitSize) {
        return;
    }

    @Override
    public boolean set(long[] indexs) {
        ensureCapacity();
        for (long bitIndex : indexs) {
            if (!get(bitIndex, data) && setBitIndex(bitIndex, data) && status == BloomFilterConstants.SINGLE) {
                bitCount.increment();
            }
            if (enableGrow && dataList.size() > 0) {
                for (int i = 0; i < dataList.size(); i++) {
                    AtomicLongArray atomicLongArray = dataList.get(i);
                    if (!get(bitIndex, atomicLongArray) && setBitIndex(bitIndex, atomicLongArray)) {
                        if (i == dataList.size() - 1) {
                            if (status == BloomFilterConstants.COPY) {
                                bitCount.increment();
                            }
                            if (status == BloomFilterConstants.NEW) {
                                long bitCount = 0;
                                for (int j = 0; j < atomicLongArray.length(); ++j) {
                                    bitCount += Long.bitCount(atomicLongArray.get(j));
                                }
                                this.bitCount = new LongAdder();
                                this.bitCount.add(bitCount);
                                this.status = BloomFilterConstants.COPY;
                            }
                        }
                    }
                }


            }
        }


        return true;
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
    public boolean get(long[] indexs) {

        for (long index : indexs) {
            if (!get(index, data)) {
                return false;
            }
        }
        if (dataList!=null&&dataList.size() > 0) {
            for (AtomicLongArray atomicLongArray : dataList) {
                for (long index : indexs) {
                    if (!get(index, atomicLongArray)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean get(long bitIndex, AtomicLongArray data) {
        return (data.get((int) (bitIndex >>> LONG_ADDRESSABLE_BITS)) & (1L << bitIndex)) != 0;
    }


    @Override
    public long bitSize() {
        return (long) data.length() * Long.SIZE;
    }

    @Override
    public void reset() {
        for (AtomicLongArray atomicLongArray : this.dataList) {
            atomicLongArray = null;
        }
        dataList = null;
        long[] longs = new long[Ints.checkedCast(LongMath.divide(bitSize, 64, RoundingMode.CEILING))];
        this.bitCount = new LongAdder();
        this.data = new AtomicLongArray(longs);
    }

    @Override
    public void clear() {
        for (AtomicLongArray atomicLongArray : this.dataList) {
            atomicLongArray=null;
        }
        dataList=null;
        this.data=null;
        this.bitCount=null;
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


    private void ensureCapacity() {
        if (enableGrow) {
            if(dataList==null){
                this.dataList = new CopyOnWriteArrayList<>();
            }
            if (bitSize() * growRate < bitCount.doubleValue()) {
                long[] longs = new long[Ints.checkedCast(LongMath.divide(bitSize, 64, RoundingMode.CEILING))];
                dataList.add(new AtomicLongArray(longs));
                this.status = BloomFilterConstants.NEW;
            }
        }
    }
}
