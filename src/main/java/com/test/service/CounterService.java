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
    RedisTemplate<String, String> redisTemplate;

    public void count(String key, int expire) {
        Long increment = redisTemplate.opsForValue().increment(key);
        System.out.println(increment);
        //hash 数据类型，分类 hs:hsk 如某路径hs下各不同ip（hsk）的访问次数
        redisTemplate.opsForHash().increment("hs", "hsk", 1);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (int i = 0; i < 3; i++) {
            count("test", 40);
        }
        System.out.println(redisTemplate.opsForHash().get("hs", "hsk"));
    }
}
