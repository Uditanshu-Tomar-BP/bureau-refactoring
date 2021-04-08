package com.bharatpe.cache.service;

import com.bharatpe.cache.DTO.AddCacheDto;
import com.bharatpe.cache.controller.cacheController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LendingCache {

    private final Logger logger = LoggerFactory.getLogger(cacheController.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;



    public Object add(AddCacheDto addCacheDto){
        if(addCacheDto.validate()){
            String key = addCacheDto.getKey();
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            final boolean hasKey = redisTemplate.hasKey(addCacheDto.getKey());

            operations.set(key, addCacheDto.getValue(), addCacheDto.getTtl(), TimeUnit.HOURS);
            logger.info(": cache insert key:{} >> value : {} ", key, addCacheDto.getValue());

            return true;
        }
        return false;
    }

    public Object get(String key){

        if(Objects.nonNull(key)){
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            final boolean hasKey = redisTemplate.hasKey(key);

            if (hasKey) {
                logger.info(": fetched from cache key:{}  ", key);
                return operations.get(key);
            }
        }

        return null;
    }

    public Object delete(String key){
        if(Objects.nonNull(key)){
            Boolean deleted = redisTemplate.delete(key);
            if (deleted != null && deleted) {
                logger.info("Key Deleted:{}", key);
            }
        }
        return null;
    }

    public boolean acquireLock(String key) {
        if (Objects.nonNull(key)) {
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            Boolean lockTaken = operations.setIfAbsent(key, "LOCK_TAKEN", Duration.ofSeconds(10));
            if (lockTaken != null && lockTaken) {
                return true;
            }
        }
        return false;
    }

    public void releaseLock(String key) {
        if (Objects.nonNull(key)) {
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            redisTemplate.delete(key);
        }
    }
}
