package com.test.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 计数器 litter demo
 */
@Component
public class CounterService implements InitializingBean {

    @Autowired
    RedisTemplate redisTemplate;

    public void count(String key, int expire) {
        Long increment = redisTemplate.opsForValue().increment(key);
        System.out.println(increment);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (int i = 0; i < 3; i++) {
            count("test", 40);
        }
    }
}
