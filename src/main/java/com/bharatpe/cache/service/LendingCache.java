package com.bharatpe.cache.service;

import com.bharatpe.cache.DTO.RequestDto;
import com.bharatpe.cache.controller.cacheController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LendingCache {

    private final Logger logger = LoggerFactory.getLogger(cacheController.class);

    @Autowired
    RedisTemplate<String, Object> redisTemplate;



    public Object add(RequestDto requestDto){
        if(requestDto.validate()){
            String key = requestDto.getKey();
            final ValueOperations<String, Object> operations = redisTemplate.opsForValue();
            final boolean hasKey = redisTemplate.hasKey(requestDto.getKey());

            operations.set(key, requestDto.getValue(), 24, TimeUnit.HOURS);
            logger.info(": cache insert key:{} >> value : {} ", key, requestDto.getValue());

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

        return false;
    }
}
