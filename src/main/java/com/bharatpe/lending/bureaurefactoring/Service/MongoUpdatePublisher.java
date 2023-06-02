package com.bharatpe.lending.bureaurefactoring.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class MongoUpdatePublisher {
    private final Logger logger = LoggerFactory.getLogger(MongoUpdatePublisher.class);

    private ObjectMapper objectMapper;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "bharatpe.update_audit";


    public <T> void publish(String clientName, String collection, String partitionKey, List<T> data, String shardKey, String shardValue) {
        logger.info("Mongo publish for client:{}, collection:{}", clientName, collection);
        sanityCheck(clientName, collection, partitionKey, data);
        List<Map<String, Object>> payload = new ArrayList<>();
        for (T row : data) {
            payload.add(getObjectMapper().convertValue(row, new TypeReference<Map<String, Object>>(){}));
        }
        Map<String, Object> payloadMap =new HashMap<>();
        payloadMap.put("client", clientName);
        payloadMap.put("collection", collection);
        payloadMap.put("data", payload);
        payloadMap.put("shardKey", shardKey);
        payloadMap.put("shardValue", shardValue);
        kafkaTemplate.send(TOPIC, partitionKey, payloadMap);
        logger.info("Published data in topic:{} for collection:{}", TOPIC, collection);
    }

    private <T> void sanityCheck(String clientName, String collection, String partitionKey, List<T> data) {
        if (StringUtils.isEmpty(clientName)) {
            throw new IllegalArgumentException("Invalid clientName!");
        }

        if (StringUtils.isEmpty(collection)) {
            throw new IllegalArgumentException("Invalid collection!");
        }

        if (ObjectUtils.isEmpty(data)) {
            throw new IllegalArgumentException("Invalid data!");
        }

        if (StringUtils.isEmpty(partitionKey)) {
            throw new IllegalArgumentException("Invalid partitionKey!");
        }
    }

    private ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        }
        return objectMapper;
    }
}
