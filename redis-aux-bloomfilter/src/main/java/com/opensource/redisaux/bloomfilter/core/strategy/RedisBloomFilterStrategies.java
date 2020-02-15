package com.opensource.redisaux.bloomfilter.core.strategy;

import com.opensource.redisaux.bloomfilter.core.bitarray.BitArray;
import com.google.common.hash.Funnel;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: lele
 * @date: 2019/12/20 上午11:39
 * 沿用guava的hash方法，通过jvm位数选择不同的hash策略，主要核心类
 */
@SuppressWarnings("unchecked")
public enum RedisBloomFilterStrategies {


    MURMUR128_MITZ_64(new Strategy() {


        @Override
        public <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bitArray) {
            long bitSize = bitArray.bitSize();
            byte[] bytes = getHash(object, funnel);
            long[] indexs = getIndex(bytes, numHashFunctions, bitSize);
            return bitArray.set(indexs);
        }

        @Override
        public <T> boolean mightContain(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bitArray
        ) {
            long bitSize = bitArray.bitSize();
            byte[] bytes = getHash(object, funnel);
            long[] indexs = getIndex(bytes, numHashFunctions, bitSize);
            return bitArray.get(indexs);
        }

        @Override
        public <T> boolean putAll(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects) {
            long bitSize = bits.bitSize();
            List<long[]> res = new LinkedList<long[]>();
            for (T object : objects) {
                byte[] bytes = getHash(object, funnel);
                res.add(getIndex(bytes, numHashFunctions, bitSize));
            }
            return bits.setBatch(res);
        }

        @Override
        public <T> List<Boolean> mightContains(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects) {
            long bitSize = bits.bitSize();
            List<long[]> res = new LinkedList<long[]>();
            for (T object : objects) {
                byte[] bytes = getHash(object, funnel);
                res.add(getIndex(bytes, numHashFunctions, bitSize));
            }
            return bits.getBatch(res);
        }


        private long lowerEight(byte[] bytes) {
            return Longs.fromBytes(bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
        }

        private long upperEight(byte[] bytes) {
            return Longs.fromBytes(bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
        }

        byte[] getHash(Object object, Funnel funnel) {
            return Hashing.murmur3_128().hashObject(object, funnel).asBytes();
        }

        private long[] getIndex(byte[] bytes, int numHashFunctions, long bitSize) {
            long hash1 = this.lowerEight(bytes);
            long hash2 = this.upperEight(bytes);
            long combinedHash = hash1;
            long[] batchIndex = new long[numHashFunctions];
            for (int i = 0; i < numHashFunctions; ++i) {
                batchIndex[i] = (combinedHash & 9223372036854775807L) % bitSize;
                combinedHash += hash2;
            }
            return batchIndex;
        }
    }, "64"),

    MURMUR128_MITZ_32(new Strategy() {
        @Override
        public <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bitArray
        ) {
            long bitSize = bitArray.bitSize();
            long hash64 = getHash(object, funnel);
            long[] indexs = getIndex(hash64, numHashFunctions, bitSize);
            return bitArray.set(indexs);

        }

        @Override
        public <T> boolean mightContain(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bitArray
        ) {
            long bitSize = bitArray.bitSize();
            long hash64 = getHash(object, funnel);
            long[] indexs = getIndex(hash64, numHashFunctions, bitSize);
            return bitArray.get(indexs);

        }

        @Override
        public <T> boolean putAll(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects) {
            long bitSize = bits.bitSize();
            List<long[]> res = new LinkedList<long[]>();
            for (T object : objects) {
                long bytes = getHash(object, funnel);
                res.add(getIndex(bytes, numHashFunctions, bitSize));
            }
            return bits.setBatch(res);
        }

        @Override
        public <T> List<Boolean> mightContains(Funnel<? super T> funnel, int numHashFunctions, BitArray bits, List<T> objects) {
            long bitSize = bits.bitSize();
            List<long[]> res = new LinkedList<long[]>();
            for (T object : objects) {
                long bytes = getHash(object, funnel);
                res.add(getIndex(bytes, numHashFunctions, bitSize));
            }
            return bits.getBatch(res);
        }


        long[] getIndex(long hash64, int numHashFunctions, long bitSize) {
            int hash1 = (int) hash64;
            int hash2 = (int) (hash64 >> 32);
            long[] batchIndex = new long[numHashFunctions];
            for (int i = 1; i <= numHashFunctions; i++) {
                int combinedHash = hash1 + (i * hash2);
                if (combinedHash < 0) {
                    combinedHash = ~combinedHash;
                }
                batchIndex[i - 1] = combinedHash % bitSize;
            }
            return batchIndex;
        }

        long getHash(Object object, Funnel funnel) {
            return Hashing.murmur3_128().hashObject(object, funnel).asLong();
        }

    }, "32");
    private String code;
    private Strategy strategy;

    RedisBloomFilterStrategies(Strategy strategy, String code) {
        this.strategy = strategy;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public static Strategy getStrategy(String code) {
        for (RedisBloomFilterStrategies customBloomFilterStrategies : values()) {
            if (customBloomFilterStrategies.getCode().equals(code)) {
                return customBloomFilterStrategies.getStrategy();
            }
        }
        return null;
    }


}