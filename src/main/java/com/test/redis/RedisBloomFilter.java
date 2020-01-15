package com.test.redis;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;


/**
 * 布隆过滤模拟
 */
public class RedisBloomFilter {
    private static final int capacity = 1000000;
    private static final int key = 999998;

    private static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), capacity);
    static {
        for (int i = 0; i < capacity; i++) {
            bloomFilter.put(i);
        }
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        if (bloomFilter.mightContain(key)) {
            System.out.println("filter:" + key);
        }
        long end = System.nanoTime();
        System.out.println("time:" + (end - start));
        int sum = 0;
        for (int i = capacity + 20000; i < capacity + 30000; i++) {
            if (bloomFilter.mightContain(i)) {
                sum = sum + 1;
            }
        }
        System.out.println("错判率为:" + sum);
    }


}
