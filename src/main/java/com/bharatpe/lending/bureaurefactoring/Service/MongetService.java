package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.Controller.BureauController;
import com.bharatpe.lending.bureaurefactoring.dto.BureauDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauRequestResponseDTO;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
@Deprecated

public class MongetService {
    Logger logger = LoggerFactory.getLogger(BureauController.class);

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    Environment env;

    @Autowired
    ObjectMapper mapper;


    public JsonNode getData(Long mobile) throws IOException {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("mobile", mobile);
            body.put("limit", 1);
            body.put("order", "DESC");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            List<String> responseBody = restTemplate.postForObject(env.getProperty("monget.api.response") + "?collection_name=bureau_data", request, List.class);

            logger.info("Get data from mongo publisher: {} ", Objects.requireNonNull(responseBody).get(0));

            return mapper.readTree(responseBody.get(0));
        }catch (Exception e){
            return null;
        }
    }

    public JsonNode getDataV2(Long mobile) throws IOException {
        try {
            Map<String,Object> filter = new HashMap<String,Object>() {{
                put("mobile", mobile.toString());
                put("bureau_type", new HashMap<String, Object> (){{ put("$ne", null); }});
            }};

            Map<String, Object> body = new HashMap<String, Object> (){{
                put("limit", 1);
                put("filter", filter);
                put("order", "DESC");
            }};
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            List<String> responseBody = restTemplate.postForObject(env.getProperty("monget.api.response.v2") + "?collection_name=bureau_data", request, List.class);

            logger.info("Get data from mongo publisher: {} ", Objects.requireNonNull(responseBody).get(0));

            return mapper.readTree(responseBody.get(0));
        }catch (Exception e){
            return null;
        }
    }

    public List<String> getTimeOutCases(Integer limit) {
        try {
            Map<String,Object> filter = new HashMap<String,Object>() {{
                put("statusCode", -1);
            }};
            Map<String,Object> requestBody = new HashMap<String,Object>() {{
                put("limit", limit);
                put("filter", filter);
                put("order", "ASC");
                put("updatedTime", new Date());
            }};
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String,Object>> request = new HttpEntity<>(requestBody, headers);
            logger.info("Request of get timeout cases: {}", request);
            // Todo: bind with dto direct
            List<String> responseEntity = restTemplate.postForObject(env.getProperty("monget.api.response.v2") + "?collection_name=bureau_data", request, List.class);
            logger.info("Get data from monget service: {} ", responseEntity);
            return responseEntity;
        }catch (Exception e){
            logger.error("Exception Occure while getting data from mongo: {}", e);
        }
        return null;
    }

    public BureauDataDTO getConsentData(String mobile) throws IOException{
        try {
            logger.info("getting consent data for mobile: {}",mobile);
            Map<String,Object> filter = new HashMap<String,Object>() {{
                put("mobile", mobile);
            }};
            Map<String, Object> body = new HashMap<String, Object> (){{
                put("limit", 1);
                put("filter", filter);
                put("order", "ASC");
            }};

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            List<String> response = restTemplate.postForObject(env.getProperty("monget.api.response.v2") + "?collection_name=bureau_data", request, List.class);
            if (Objects.nonNull(response) && Objects.nonNull(response.get(0))) {
                JsonNode jsonResponse = mapper.readTree(response.get(0));
                BureauDataDTO bureauDataDTO = mapper.readValue(mapper.writeValueAsString(jsonResponse), BureauDataDTO.class);
                logger.info("Merchant consent found for merchant with phone: {} is: {}", mobile, bureauDataDTO);
                return bureauDataDTO;
            }
        }catch (Exception e){
            logger.error("Exception while getting consent from mongo with phone: {} {} {}", mobile, Arrays.asList(e.getStackTrace()),e);
        }
        return null;
    }

    public List<BureauRequestResponseDTO> getRequestResponse(String mobile, Bureau bureau, String stage, Integer limit) throws IOException {
        try {
            logger.info("getting request response data data for mobile: {}",mobile);
            Map<String,Object> filter = new HashMap<String,Object>() {{
                put("mobile", mobile);
                put("bureau_type", bureau);
            }};

            if (Objects.nonNull(stage))
                filter.put("stage", stage);

            Map<String, Object> body = new HashMap<String, Object> (){{
                put("limit", limit);
                put("filter", filter);
                put("order", "DESC");
            }};

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            List<String> responses = restTemplate.postForObject(env.getProperty("monget.api.response.v2") + "?collection_name=bureau_request_response", request, List.class);
            List<BureauRequestResponseDTO> bureauRequestResponseDTOS = new ArrayList<>();
            for(String response: responses) {
                if (Objects.nonNull(response)) {
                    JsonNode jsonResponse = mapper.readTree(response);
                    BureauRequestResponseDTO bureauRequestResponseDTO = mapper.readValue(mapper.writeValueAsString(jsonResponse), BureauRequestResponseDTO.class);
                    logger.info("Bureau request response found for merchant with phone: {} is: {}", mobile, bureauRequestResponseDTO);
                    bureauRequestResponseDTOS.add(bureauRequestResponseDTO);
                }
            }
            return bureauRequestResponseDTOS;

        }catch (Exception e){
            logger.error("Exception while getting request response from mongo with phone: {} {} {}", mobile, Arrays.asList(e.getStackTrace()),e);
        }
        return null;
    }
}
