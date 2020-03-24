package com.test.service;

import com.test.dao.DataOperation;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 缓存击穿：某些热点key，在大并发请求下突然失效，从而“击穿缓存”，转而请求到数据库，造成数据库可能崩溃
 * 解决方案：
 * 1、使用互斥锁
 */
@Component
@Slf4j
public class RedisCacheDataBreak {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

//    List<InetSocketAddress> address = new ArrayList();
//    MemcachedClient memcachedClient;
//    {
//        try {
//            memcachedClient = new MemcachedClient(address);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    /**
     * 缓存击穿方案一：互斥锁 mutex - 使用jedis的互斥锁
     * 问题：吞吐量有所降低
     * @param key
     * @param lockKey
     * @param uniqueId
     * @param expireTime
     * @return
     */
    public String redisMutex(String key, String lockKey, String uniqueId, long expireTime) {
        //search cache
        String result = redisTemplate.opsForValue().get(key);
        if (result == null) {
            try {
                //这里也可以设置只对Key的锁，expireTime表示设置锁的超时
                boolean locked = RedisMutex.tryDistributedLock(lockKey, uniqueId, expireTime);
                if (locked) {
                    //search data source
                    result = DataOperation.getDBValue(key);
                    redisTemplate.opsForValue().set(key, result);
                    redisTemplate.delete(lockKey);
                    return result;
                } else {
                    // 其它线程进来了没获取到锁便等待50ms，等待其他线程回设缓存，再重试
                    Thread.sleep(50);
                    redisMutex(key, lockKey, uniqueId, expireTime);
                }
            } catch (Exception e) {
                log.error("getWithLock exception:" + e);
                return result;
            } finally {
                RedisMutex.releaseDistributedLock(lockKey, uniqueId);
            }
        }
        return result;
    }

//    /**
//     * 缓存击穿方案一：互斥锁 mutex - 使用memcached的互斥锁
//     * @param key
//     * @param value
//     * @return
//     * @throws InterruptedException
//     */
//    public String memcachedMutex(String key, String value) throws InterruptedException {
//        //search cache
//        String result = redisTemplate.opsForValue().get(key);
//        if (result == null) {
//            if (memcachedClient.add(key, 3 * 60 * 1000, value).isDone()) {
//                //search data source
//                result = DataOperation.getDBValue(key);
//                redisTemplate.opsForValue().set(key, result);
//                redisTemplate.delete(key);
//                return result;
//            } else {
//                // 其它线程进来了没获取到锁便等待50ms，等待其他线程回设缓存，再重试
//                Thread.sleep(50);
//                memcachedMutex(key, value);
//            }
//        }
//        return result;
//    }
}
