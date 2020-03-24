package com.test.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.test.dao.DataOperation;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

/**
 * 缓存穿透问题：大量并发请求的key不存在缓存中，请求直接穿透缓存请求到数据库，造成数据库数据处理压力，可能崩溃
 * 解决方案：
 * 1、布隆过滤器：对请求参数进入到缓存层前进行过滤（需要提前将可能的请求参数加入到过滤器中）。重要使用方式之一
 * 2、对于无效的key，设置短过期时间（部分场景可用）
 */

@Component
public class RedisCachePenetration {
    @Autowired
    RedisTemplate<String, String> redisTemplate;


    private static final int capacity = 1000000;
    private static BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charset.forName("utf-8")), capacity);


    //解决缓存穿透方案之一：空值也加入缓存，但是设置短有效期
    public String dataPenetrate(String key) {
        //search cache
        String result = redisTemplate.opsForValue().get(key);
        if (result != null) {
            return result;
        }
        //search data source
        result = DataOperation.getDBValue(key);
        if (result != null) {
            redisTemplate.opsForValue().set(key, result);
        } else {
            //即使返回对象为空，也放入缓存中，但是设置一个较短的有效期
            redisTemplate.opsForValue().set(key, result, 60, TimeUnit.SECONDS);
        }
        return result;
    }

    /**
     * 解决缓存穿透方案之一：布隆过滤器
     * 缺点：存在误报率，不过相对较低
     * @param key
     * @return
     */
    public String getValueByBloomFilter(String key) {
        // 通过key获取value
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtil.isNullOrEmpty(value)) {
            if (bloomFilter.mightContain(key)) {
                String cacheValue = redisTemplate.opsForValue().get(key);
                if (cacheValue != null) {
                    return cacheValue;
                } else {
                    value = DataOperation.getDBValue(key);
                    redisTemplate.opsForValue().set(key, value);
                    return value;
                }
            } else {
                return null;
            }
        }
        return "param error";
    }
}
