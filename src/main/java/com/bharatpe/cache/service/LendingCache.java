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
import java.util.Date;
import java.util.List;
import java.util.Map;
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

        try {
            if(Objects.nonNull(key)){
                final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
                final boolean hasKey = Boolean.TRUE.equals(redisTemplate.hasKey(key));

                if (hasKey) {
                    logger.info(": fetched from cache key:{}  ", key);

                    Object value = operations.get(key);

                    logger.info("fetched from key:{} value : {} ", key, value.toString());

                    return value;
                }
            }
        } catch (Exception e) {
            logger.info("Error occurred while fetching the value for key : {}", key, e);
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

    public Boolean isKeyExist(String key) {
        return redisTemplate.hasKey(key);
    }

    public void updateHash(String key, Map<String, String> mapObj, Date expiry) {
        redisTemplate.opsForHash().putAll(key, mapObj);
        redisTemplate.expireAt(key, expiry);
    }

    public Map<Object, Object> getHashEntries(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public Boolean removeHashKey(String key) {
        return redisTemplate.delete(key);
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
    public Object popValueFromSet(String key) {
        Assert.notNull(key, "key is required");
        Object value = this.redisTemplate.opsForSet().pop(key);
        logger.info(": cache pop individual from set key:{} >> value : {} ", key, value);
        return value;
    }

    public List<Object> popValuesFromSet(String key, Long count) {
        Assert.notNull(key, "key is required");
        Assert.notNull(count, "count is required");
        List<Object> values = this.redisTemplate.opsForSet().pop(key, count);
        logger.info(": cache pop values from set key:{}", key);
        return values;
    }

    public Long sizeOfSet(String key) {
        Assert.notNull(key, "key is required");
        Long size = redisTemplate.opsForSet().size(key);
        logger.info(": cache set size: {} of key:{}", size, key);
        return size;
    }
}
