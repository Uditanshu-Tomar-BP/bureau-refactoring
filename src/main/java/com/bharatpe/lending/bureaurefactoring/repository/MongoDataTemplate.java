package com.bharatpe.lending.bureaurefactoring.repository;

import com.bharatpe.lending.bureaurefactoring.dto.BureauDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauRequestResponseDTO;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

//import javax.management.Query;
//import java.util.Date;
import java.util.List;
//import java.util.Objects;

@Component
public class MongoDataTemplate {
    private final MongoTemplate mongoTemplate;
    private final Environment env;
    private final ObjectMapper mapper;
    private final Logger logger = LoggerFactory.getLogger(MongoDataTemplate.class);

    @Autowired
    public MongoDataTemplate(MongoTemplate mongoTemplate, Environment env, ObjectMapper mapper) {
        this.mongoTemplate = mongoTemplate;
        this.env = env;
        this.mapper = mapper;
    }

    public JsonNode getData(Long mobile) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("mobile").is(mobile));
            query.limit(1);
            query.with(Sort.by(Sort.Direction.DESC, "_id"));

            List<String> responseBody = mongoTemplate.find(query, String.class, "bureau_data");

            if (responseBody != null && !responseBody.isEmpty()) {
                String firstResponse = responseBody.get(0);
                return mapper.readTree(firstResponse);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode getDataV2(Long mobile) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("mobile").is(mobile.toString())
                    .and("bureau_type").ne(null));
            query.limit(1);
            query.with(Sort.by(Sort.Direction.DESC, "_id"));

            List<String> responseBody = mongoTemplate.find(query, String.class, "bureau_data");

            if (responseBody != null && !responseBody.isEmpty()) {
                String firstResponse = responseBody.get(0);
                return mapper.readTree(firstResponse);
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getTimeOutCases(Integer limit) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("statusCode").is(-1));
            query.limit(limit);
            query.with(Sort.by(Sort.Direction.ASC, "updatedTime"));

            List<String> responseEntity = mongoTemplate.find(query, String.class, "bureau_data");
            logger.info("Get data from monget service: {} ", responseEntity);
            return responseEntity;
        } catch (Exception e) {
            logger.error("Exception occurred while getting data from mongo: {}", e);
        }
        return null;
    }

    public BureauDataDTO getConsentData(String mobile) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("mobile").is(mobile));
            query.limit(1);
            query.with(Sort.by(Sort.Direction.ASC, "_id"));

            List<String> response = mongoTemplate.find(query, String.class, "bureau_data");
            if (response != null && !response.isEmpty()) {
                String firstResponse = response.get(0);
                JsonNode jsonResponse = mapper.readTree(firstResponse);
                BureauDataDTO bureauDataDTO = mapper.convertValue(jsonResponse, BureauDataDTO.class);

                logger.info("Merchant consent found for merchant with phone: {} is: {}", mobile, bureauDataDTO);
                return bureauDataDTO;
            }
        } catch (Exception e) {
            logger.error("Exception while getting consent from mongo with phone: {} {}", mobile, e);
        }
        return null;
    }

    public List<BureauRequestResponseDTO> getRequestResponse(String mobile, Bureau bureau, String stage, Integer limit) {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("mobile").is(mobile)
                    .and("bureau_type").is(bureau)
                    .and("stage").is(stage));
            query.limit(limit);
            query.with(Sort.by(Sort.Direction.DESC, "_id"));

            List<String> responses = mongoTemplate.find(query, String.class, "bureau_request_response");
            List<BureauRequestResponseDTO> bureauRequestResponseDTOS = mapper.convertValue(responses, mapper.getTypeFactory().constructCollectionType(List.class, BureauRequestResponseDTO.class));
            logger.info("Bureau request response found for merchant with phone: {} is: {}", mobile, bureauRequestResponseDTOS);
            return bureauRequestResponseDTOS;
        } catch (Exception e) {
            logger.error("Exception while getting request response from mongo with phone: {} {}", mobile, e);
        }
        return null;
    }
}
