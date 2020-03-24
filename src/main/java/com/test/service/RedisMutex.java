package com.test.service;

import redis.clients.jedis.Jedis;

import java.util.Collections;

/**
 * 锁
 */
public class RedisMutex {
    private static final String LOCKED_SUCCESS = "OK";
    //可选项:NX、XX  其中NX表示当key不存在时才set值，XX表示当key存在时才set值
    private static final String NX = "NX";
    //过期时间单位，可选项:EX|PX 其中EX为seconds,PX为milliseconds
    private static final String EXPIRE_TIME = "PX";
    //返回值1代表成功
    private static final Long RELEASE_SUCCESS = 1L;

    private static Jedis jedis = new Jedis();


    /**
     * 获取锁
     *
     * @param jedis      redis客户端
     * @param lockKey    锁的key
     * @param uniqueId   请求标识（比如UUID之类的，用来表示当前请求客户端的唯一性标识）
     * @param expireTime 过期时间
     * @return 是否获取锁
     */
    public static boolean tryDistributedLock(String lockKey, String uniqueId, long expireTime) {
//        //错误实现
//        //setnx方法表示当key不存在时才set值，存在不做任何操作
//        Long result = jedis.setnx(lockKey, uniqueId);
//        //设置过期时间，但是如果此时服务器宕机，将无法释放锁（这两个setnx和expire并不具备原子性）
//        if (result == 1) {
//            jedis.expire(lockKey, expireTime);
//        }

        String result = jedis.set(lockKey, uniqueId, NX, EXPIRE_TIME, expireTime);
        return LOCKED_SUCCESS.equals(result);
    }


    /**
     * 释放锁
     * @param jedis redis客户端
     * @param lockKey 锁的key
     * @param uniqueId 请求标识（比如UUID之类的，用来表示当前请求客户端的唯一性标识）
     * @return 是否释放
     */
    public static boolean releaseDistributedLock(String lockKey, String uniqueId) {
        String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(luaScript, Collections.singletonList(lockKey), Collections.singletonList(uniqueId));
        return RELEASE_SUCCESS.equals(result);
    }

}
