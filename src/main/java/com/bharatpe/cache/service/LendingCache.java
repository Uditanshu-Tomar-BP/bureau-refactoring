package com.bharatpe.cache.service;

import com.bharatpe.cache.DTO.AddCacheDto;
import com.bharatpe.cache.controller.cacheController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
            final boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(addCacheDto.getKey()));

            operations.set(key, addCacheDto.getValue(), addCacheDto.getTtl(), TimeUnit.HOURS);
            logger.info(": cache insert key:{} >> value : {} ", key, addCacheDto.getValue());

            return true;
        }
        return false;
    }

    public Object add(AddCacheDto addCacheDto, TimeUnit timeUnit){
        if(addCacheDto.validate()){
            String key = addCacheDto.getKey();
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            operations.set(key, addCacheDto.getValue(), addCacheDto.getTtl(), timeUnit);
            logger.info(": cache insert key:{} >> value : {} ", key, addCacheDto.getValue());
            return true;
        }
        return false;
    }

    public Object get(String key){

        if(Objects.nonNull(key)){
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            final boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(key));

            if (hasKey) {
                logger.info(": fetched from cache key:{}  ", key);

                Object value = operations.get(key);

                logger.info("fetched from key:{}   value : {} ", key, value);

                return value;
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

    public Boolean contains(String key, Object value) {
        Assert.notNull(key, "key is required");
        Assert.notNull(value, "value is required");
        return redisTemplate.opsForSet().isMember(key, value);
    }

    public boolean acquireLock(String key) {
        return acquireLock(key, 10);
    }

    public boolean acquireLock(String key, Integer ttl) {
        if (Objects.nonNull(key)) {
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            Boolean lockTaken = operations.setIfAbsent(key, "LOCK_TAKEN", Duration.ofSeconds(ttl));
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

    public Boolean addValue(String key, Object value) {
        Assert.notNull(key, "key is required");
        Assert.notNull(value, "value is required");
        Long add = redisTemplate.opsForSet().add(key, value);
        logger.info(": cache insert individual in set key:{} >> value : {} ", key, value);
        return Objects.nonNull(add) && add > 0L;
    }

    public Boolean removeValue(String key, Object value) {
        Assert.notNull(key, "key is required");
        Assert.notNull(value, "value is required");

        Long delete = redisTemplate.opsForSet().remove(key, value);
        logger.info(": cache delete individual from set key:{} >> value : {} ", key, value);
        return Objects.nonNull(delete) && delete > 0L;
    }
}
