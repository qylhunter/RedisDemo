package com.test.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;


/**
 * 布隆过滤器模拟：guava自带
 * 布隆过滤器缺点：存在误报率，不过相对较低
 */
@Component
public class RedisBloomFilter {
    private static final int capacity = 1000000;
    private static final int key = 999998;

    public static boolean isExistBloomFilter(String key) {
        return stringBloomFilter.mightContain(key);
    }

    private static BloomFilter<Integer> bloomFilter = BloomFilter.create(Funnels.integerFunnel(), capacity);
    private static BloomFilter<String> stringBloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), capacity);
    static {
        for (int i = 0; i < capacity; i++) {
            bloomFilter.put(i);
        }
    }

    //测试误报率
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
