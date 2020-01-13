package com.test.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class RedisQuestion {
    @Autowired
    RedisTemplate redisTemplate;


    //解决数据穿透方案之一
    public Data dataPenetrate(String id) {
        //search cache
        Object o = redisTemplate.opsForValue().get(id);
        if (o != null) {
            return (Data) o;
        }
        //search data source
        Data obj = dataSource(id);
        if (obj != null) {
            redisTemplate.opsForValue().set(id, obj);
        } else {
            //即使返回对象为空，也放入缓存中，但是设置一个较短的有效期
            redisTemplate.opsForValue().set(id, obj, 60, TimeUnit.SECONDS);
        }
        return obj;
    }

    //解决雪崩方案之一
    public Data avalanche(String id) {
        //search cache
        Object o = redisTemplate.opsForValue().get(id);
        if (o != null) {
            return (Data) o;
        }
        //search data source
        Data obj = dataSource(id);
        if (obj != null) {
            Random random = new Random();
            //根据种类不同，设置不同的有效期
            if (obj.getFlag() == 1) {
                int time = 360 + random.nextInt(360);
                redisTemplate.opsForValue().set(id, obj, time, TimeUnit.SECONDS);
            } else {
                int time = 60 + random.nextInt(360);
                redisTemplate.opsForValue().set(id, obj, time, TimeUnit.SECONDS);
            }
        } else {
            //即使返回对象为空，也放入缓存中，但是设置一个较短的有效期
            redisTemplate.opsForValue().set(id, obj, 60, TimeUnit.SECONDS);
        }
        return obj;
    }

//    private synchronized Object getCache(String id) {
//        return redisTemplate.opsForValue().get(id);
//    }

    private Data dataSource(String id) {
        return new Data();
    }

    class Data {
        String id;
        String content;
        int flag;

        public int getFlag() {
            return flag;
        }

        public void setFlag(int flag) {
            this.flag = flag;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }



}
