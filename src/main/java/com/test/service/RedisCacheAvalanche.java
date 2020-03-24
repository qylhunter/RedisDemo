package com.test.service;

import com.test.dao.DataOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 缓存雪崩：缓存同一时间大面积失效，造成大量请求集中到DB上，从而造成DB崩溃
 * 解决方案：
 * 1、设置不同种类缓存，根据不同的分类都设置不同的失效时间；
 * 2、
 * 1）事前：设置集群，保证redis高可用性；设置合适的内存淘汰策略
 * 2）事中：本地缓存+hystrix限流降级或者锁+队列（影响吞吐量）
 * 3）事后：尽快恢复持久化的数据，恢复缓存
 */

@Component
@Slf4j
public class RedisCacheAvalanche {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    /**
     * 解决雪崩方案之一：设置不同种类及有效期
     *
     * @param key
     * @return
     */
    public void methodOne(String key, String value) {
        Random random = new Random();
        //根据种类不同，设置不同的有效期
        if (key.equals("1")) {
            int time = 360 + random.nextInt(360);
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else if (key.equals("2")) {
            int time = 60 + random.nextInt(360);
            redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
        } else {
            //不在分类中的缓存，设置一个较短的有效期
            redisTemplate.opsForValue().set(key, value, 60, TimeUnit.SECONDS);
        }
    }

    /**
     * 解决雪崩方案之二：锁
     * 影响性能，高并发下少使用
     */
    public String cacheAndLock() {
        int cacheTime = 30;
        String cacheKey = "product_list";
        String lockKey = cacheKey;

        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        if (cacheValue != null) {
            return cacheValue;
        } else {
            synchronized (lockKey) {
                cacheValue = redisTemplate.opsForValue().get(cacheKey);
                if (cacheValue != null) {
                    return cacheValue;
                } else {
                    //这里一般是sql查询数据
                    cacheValue = DataOperation.getDBValue(cacheKey);
                    redisTemplate.opsForValue().set(cacheKey, cacheValue, cacheTime);
                }
            }
            return cacheValue;
        }
    }

    /**
     * 解决雪崩方案之三：设置过期标记
     */
    public String cacheAndFlag(String key) {
        int cacheTime = 30;
        //缓存标记
        String cacheSign = key + "_sign";

        String sign = redisTemplate.opsForValue().get(cacheSign);
        //获取缓存值
        String cacheValue = redisTemplate.opsForValue().get(key);
        if (sign != null) {
            return cacheValue; //未过期，直接返回
        } else {
            //设置旧数据，等待新数据更新
            redisTemplate.opsForValue().set(cacheSign, cacheValue, cacheTime);
            //这里一般是 sql查询数据
            cacheValue = DataOperation.getDBValue(key);
            //日期设缓存时间的2倍，用于脏读
            redisTemplate.opsForValue().set(key, cacheValue, cacheTime * 2);
            //此时再更新缓存标志
            redisTemplate.opsForValue().set(cacheSign, cacheValue, cacheTime);
            return cacheValue;
        }
    }

    /**
     * 解决雪崩方案之四：设置多级缓存
     */

}
