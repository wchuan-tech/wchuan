package com.wchuan.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisCache {

    public final RedisTemplate redisTemplate;

    public <T> void setCacheObject(String key, T value){
        redisTemplate.opsForValue().set(key,value);
    }

    public <T> void setCacheObject(final String key,final T value,final Integer timeout,final TimeUnit TimeUnit){
        redisTemplate.opsForValue().set(key,value,timeout,TimeUnit);
    }

    public boolean expire(final String key,final long timeout){
        return expire(key,timeout,TimeUnit.SECONDS);
    }

    public boolean expire(final String key,final long timeout,final TimeUnit unit){
        return redisTemplate.expire(key,timeout,unit);
    }

    /**
     * 获取对象的剩余存活时间 (默认单位：秒)
     *
     * @param key Redis键
     * @return 剩余过期时间（秒）。
     *         返回 -1 表示永久有效；
     *         返回 -2 表示键不存在。
     */
    public Long getExpireTime(final String key) {
        return redisTemplate.getExpire(key);
    }

    /**
     * 获取对象的剩余存活时间，并指定返回的时间单位
     *
     * @param key  Redis键
     * @param unit 时间单位 (例如 TimeUnit.MINUTES)
     * @return 剩余过期时间
     */
    public Long getExpireTime(final String key, final TimeUnit unit) {
        return redisTemplate.getExpire(key, unit);
    }

    public <T> T getCacheObject(final String key){
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    public boolean deleteObject(final String key){
        return redisTemplate.delete(key);
    }

    public long deleteObjects(final Collection collection){
        return redisTemplate.delete(collection);
    }

    public <T> long setCacheList(final String key, final List<T> dataList){
        Long count = redisTemplate.opsForList().rightPush(key, dataList);
        return count == null ? 0 : count;
    }

    public <T> List<T> getCacheList(final String key){
        return redisTemplate.opsForList().range(key,0,-1);
    }

    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet){
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> iterator = dataSet.iterator();

        while(iterator.hasNext()){
            setOperation.add(iterator.next());
        }

        return setOperation;
    }

    public <T> Set<T> getCacheSet(final String key){
        return redisTemplate.opsForSet().members(key);
    }

    public <T> void setCacheMap(final String key, final Map<String, T> dataMap){
        if(dataMap == null){
            redisTemplate.opsForHash().putAll(key,dataMap);
        }
    }

    public <T> Map<String,T> getCacheMap(final String key){
        return redisTemplate.opsForHash().entries(key);
    }

    public <T> void setCacheMapValue(final String key,final String hKey,final T value){
        redisTemplate.opsForHash().put(key,hKey,value);
    }

    public <T> T getCacheMapValue(final String key,final String hKey){
        HashOperations<String, String, T> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(key,hKey);
    }

    public void delCacheMapValue(final String key,final String hKey){
        HashOperations hashOperations = redisTemplate.opsForHash();
        hashOperations.delete(key,hKey);
    }

    public <T> List<T> getMultiCacheMapValue(final String key,final Collection<Object> hKeys){
        return redisTemplate.opsForHash().multiGet(key,hKeys);
    }

    public Collection<String> keys(final String patter){
        return redisTemplate.keys(patter);
    }
}
