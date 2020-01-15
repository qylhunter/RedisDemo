package com.test.redis;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.nio.charset.Charset;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RedisQuestion {
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
        result = getDBValue(key);
        if (result != null) {
            redisTemplate.opsForValue().set(key, result);
        } else {
            //即使返回对象为空，也放入缓存中，但是设置一个较短的有效期
            redisTemplate.opsForValue().set(key, result, 60, TimeUnit.SECONDS);
        }
        return result;
    }

    /**
     * 解决雪崩方案之一：设置不同种类及有效期
     * @param key
     * @return
     */
    public String avalanche(String key) {
        //search cache
        String result = redisTemplate.opsForValue().get(key);
        if (result != null) {
            return result;
        }
        //search data source
        result = getDBValue(key);
        if (result != null) {
            Random random = new Random();
            //根据种类不同，设置不同的有效期
            if (result.equals("1")) {
                int time = 360 + random.nextInt(360);
                redisTemplate.opsForValue().set(key, result, time, TimeUnit.SECONDS);
            } else {
                int time = 60 + random.nextInt(360);
                redisTemplate.opsForValue().set(key, result, time, TimeUnit.SECONDS);
            }
        } else {
            //即使返回对象为空，也放入缓存中，但是设置一个较短的有效期
            redisTemplate.opsForValue().set(key, result, 60, TimeUnit.SECONDS);
        }
        return result;
    }

    /**
     * 缓存击穿方案一：互斥锁
     * 问题：吞吐量有所降低
     * @param key
     * @param jedis
     * @param lockKey
     * @param uniqueId
     * @param expireTime
     * @return
     */
    public String dataBreak(String key, Jedis jedis, String lockKey, String uniqueId, long expireTime) {
        //search cache
        String result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            try {
                boolean locked = RedisLock.tryDistributedLock(jedis, lockKey, uniqueId, expireTime);
                if (locked) {
                    //search data source
                    result = getDBValue(key);
                    redisTemplate.opsForValue().set(key, result);
                    redisTemplate.delete(lockKey);
                    return result;
                } else {
                    // 其它线程进来了没获取到锁便等待50ms后重试
                    Thread.sleep(50);
                    dataBreak(key, jedis, lockKey, uniqueId, expireTime);
                }
            } catch (Exception e) {
                log.error("getWithLock exception:" + e);
                return result;
            } finally {
                RedisLock.releaseDistributedLock(jedis, lockKey, uniqueId);
            }
        }
        return result;

    }

    /**
     * 缓存击穿/穿透方案：布隆过滤器
     * @param key
     * @return
     */
    public String getValueByBloomFilter(String key) {
        // 通过key获取value
        String value = redisTemplate.opsForValue().get(key);
        if (StringUtil.isNullOrEmpty(value)) {
            if (bloomFilter.mightContain(key)) {
                value = getDBValue(key);
                redisTemplate.opsForValue().set(key, value);
                return value;
            } else {
                return null;
            }
        }
        return value;
    }

    private String getDBValue(String key) {
        return "db_value";
    }

}
