package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.common.utils.HmacCalculator;
import com.bharatpe.lending.bureaurefactoring.config.AppProperty;
import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.bharatpe.lending.bureaurefactoring.dto.HistoricalBureauDataDto;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusCode;
import com.bharatpe.lending.bureaurefactoring.exception.BureauClientException;
import com.bharatpe.lending.bureaurefactoring.exception.BureauParsingException;
import com.bharatpe.lending.bureaurefactoring.utils.CommonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONObject;
import org.joda.time.DateTime;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
@Service
public class BureauAPIGatewayService {
    Logger logger = LoggerFactory.getLogger(BureauAPIGatewayService.class);

    @Autowired
    Environment env;

    @Autowired
    @Qualifier("bureauTemplate")
    RestTemplate restTemplate;

    @Autowired
    HmacCalculator hmacCalculator;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    BureauCommonService bureauCommonService;


    @Autowired
    CommonUtils commonUtils;

    @Value("${lending.global.base.url}")
    String baseUrl;

    @Value("${bureau.client.name:LENDING}")
    String clientName;

    @Autowired
    BureauResponseService bureauResponseService;

    @Autowired
    AppProperty appProperty;

    public String fetchExperianDetails(HttpEntity<MultiValueMap<String, Object>> bureauRequest, Long mobile) throws BureauClientException {
        Long a = DateTime.now().getMillis();
        logger.info("Experian request:{} for mobile:{}", bureauRequest, mobile);
        try {
            String response = restTemplate.postForObject(BureauConstants.SHORT_API_URL, bureauRequest, String.class);
            logger.info("Experian response:{} for mobile:{}", response, mobile);
            Long b = DateTime.now().getMillis();
            logger.info("Experian Short API response time---{}ms", (b - a));
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            logger.info("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
            if (e.getCause() instanceof SocketTimeoutException)
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), bureauRequest, null);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        }
    }

    public String fetchMaskedExperianDetails(HttpEntity<MultiValueMap<String, Object>> bureauRequest) throws BureauClientException {
        Long a = DateTime.now().getMillis();
        logger.info("Experian request:{} for fetching masked mobile numbers", bureauRequest);
        try {
            String response = restTemplate.postForObject(BureauConstants.EXPERIAN_MASK_API_URL, bureauRequest, String.class);
            logger.info("Experian response:{} with masked mobile numbers", response);
            Long b = DateTime.now().getMillis();
            logger.info("Experian Masked API response time---{}ms", (b - a));
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian masked api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            logger.info("ResourceAccessException while experian masked api response : {}, {}", e.getMessage(), e);
            if (e.getCause() instanceof SocketTimeoutException)
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), bureauRequest, null);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        }
    }

    public String fetchAuthenticatedMaskedExperianDetails(HttpEntity<MultiValueMap<String, Object>> bureauRequest, Long mobile) throws BureauClientException {
        Long a = DateTime.now().getMillis();
        logger.info("Experian request:{} for mapped mobile number:{}", bureauRequest, mobile);
        try {
            String response = restTemplate.postForObject(BureauConstants.AUTHENTICATE_DELIVERY_DATA_API_URL, bureauRequest, String.class);
            logger.info("Experian response:{} for mapped mobile number:{}", response, mobile);
            Long b = DateTime.now().getMillis();
            logger.info("Experian Authenticate Masked Mobile API response time---{}ms", (b - a));
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian Authenticate Masked Mobile Api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            logger.info("ResourceAccessException while experian Authenticate Masked Mobile Api response : {}, {}", e.getMessage(), e);
            if (e.getCause() instanceof SocketTimeoutException)
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), bureauRequest, null);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        }
    }

    public JsonNode setHitId(JsonNode bureauResponse, String rawResponse) throws BureauParsingException {
        try {
            JsonNode jsonNode = mapper.readTree(rawResponse);
            if (jsonNode == null || jsonNode.get("showHtmlReportForCreditReport").isNull()) {
                return null;
            }
            JsonNode jsonNode1 = mapper.readTree(bureauResponse.toString());
            if(jsonNode.hasNonNull("stgOneHitId")){
                ((ObjectNode) jsonNode1).put("hit_id", jsonNode.get("stgOneHitId").asText());
                return jsonNode1;
            }
            if(jsonNode.hasNonNull("stageOneId_")) {
                ((ObjectNode) jsonNode1).put("hit_id", jsonNode.get("stageOneId_").asText());
            }
            return jsonNode1;
        } catch (Exception e) {
            logger.error("Error Parsing Bureau Experian Response : {}, {}", e.getMessage(), e);
            throw new BureauParsingException(e.getLocalizedMessage(), e);
        }
    }

    public JsonNode parseExperianResponse(String rawResponse) throws BureauParsingException {
        try {
            JsonNode jsonNode = mapper.readTree(rawResponse);
            if(jsonNode == null) return  null;
            if(jsonNode.hasNonNull("errorString")) {
                return jsonNode;
            }
            if(jsonNode.get("showHtmlReportForCreditReport").isNull()) return null;
            String xmlResponse = jsonNode.get("showHtmlReportForCreditReport").asText()
                    .replaceAll("&amp;", "&")
                    .replaceAll("&gt;", ">")
                    .replaceAll("&lt;", "<")
                    .replaceAll("&quot;", "\"");
            JSONObject jsonObject = XML.toJSONObject(xmlResponse);
            return mapper.readTree(jsonObject.toString());
        } catch (Exception e) {
            logger.error("Error Parsing Bureau Experian Response : {}, {}", e.getMessage(), e);
            throw new BureauParsingException(e.getLocalizedMessage(), e);
        }
    }

    public JsonNode parseExperianMaskedMobileApiResponse(String rawResponse) throws BureauParsingException {
        try {
            return mapper.readTree(rawResponse);
        } catch (Exception e) {
            logger.error("Error Parsing Bureau Experian Response : {}, {}", e.getMessage(), e);
            throw new BureauParsingException(e.getLocalizedMessage(), e);
        }
    }

    public String experianRefreshApi(String hitId) throws BureauClientException {
        logger.info("Calling Experian Refresh API with hitId:{}", hitId);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("clientName", BureauConstants.CLIENT_NAME);
        body.add("hitId", hitId);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body);
        Long a = DateTime.now().getMillis();
        try {
            String response = restTemplate.postForObject(BureauConstants.REFRESH_API_URL, request, String.class);
            Long b = DateTime.now().getMillis();
            logger.info("Experian Refresh API response time---{}ms", (b - a));
            return response;
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                logger.info("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), request, null);
            }
            logger.error("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        }
    }

    public void setExperianApiParams(MultiValueMap<String, Object> body, String firstName, String lastName, String contact, String panCard) {
        body.add("clientName", BureauConstants.CLIENT_NAME);
        body.add("allowInput", "1");
        body.add("allowEdit", "1");
        body.add("allowCaptcha", "1");
        body.add("allowConsent", "1");
        body.add("allowEmailVerify", "1");
        body.add("allowVoucher", "1");
        body.add("voucherCode", appProperty.getVoucherCode());
        body.add("firstName", firstName);
        body.add("surName", lastName);
        body.add("mobileNo", contact);
        body.add("noValidationByPass", "0");
        body.add("emailConditionalByPass", "1");
        if (panCard != null) {
            body.add("pan", panCard);
        }
    }

    public void setExperianAuthenticatedMaskedMobileApiParams(MultiValueMap<String, Object> body, String stageOneHitId, String stageTwoHitId, String mobile) {
        body.add("stgOneHitId", stageOneHitId);
        body.add("stgTwoHitId", stageTwoHitId);
        body.add("ActualMobileNumber", mobile);
    }

    public void setExperianApiParams(MultiValueMap<String, Object> body, String stgOneHitId, String stgTwoHitId) {
        body.add("clientName", BureauConstants.CLIENT_NAME);
        body.add("stgOneHitId", stgOneHitId);
        body.add("stgTwoHitId", stgTwoHitId);
    }


//    public JsonNode getCrifReport(String panCard, Long mobile, String firstName, String lastName) {
//        JsonNode stage1Response = crifStage1(firstName, lastName, panCard, mobile.toString());
//        if (stage1Response != null && stage1Response.get("status") != null && stage1Response.get("status").asText().equals("S06")) {
//            logger.info("Crif stage1 success for mobile:{}", mobile);
//            JsonNode stage2Response = crifStage2(mobile, stage1Response.get("orderId").asText(), stage1Response.get("reportId").asText(), stage1Response.get("redirectURL").asText(), false, "");
//            if (stage2Response != null && stage2Response.get("status") != null && (stage2Response.get("status").asText().equals("S10") || stage2Response.get("status").asText().equals("S01"))) {
//                logger.info("Crif stage2 success for mobile:{}", mobile);
//                JsonNode stage3Response = crifStage2(mobile, stage1Response.get("orderId").asText(), stage1Response.get("reportId").asText(), stage1Response.get("redirectURL").asText(), true, "");
//                if (stage3Response != null) {
//                    logger.info("Found crif report for mobile:{}", mobile);
//                    if (bureauCommonService.isValidReport(panCard, mobile.toString(), stage3Response)) {
//                        return stage3Response;
//                    } else {
//                        logger.info("Invalid crif report for mobile:{}", mobile);
//                    }
//                }
//            } else {
//                return null;
//            }
//        }
//        return null;
//    }

    public String crifStage1(String firstName, String lastName, String pancard, String mobile) throws BureauClientException {
        HttpEntity<String> request = null;
        try {
            logger.info("Calling CRIF stage1 api for mobile:{} with pancard:{}", mobile, pancard);
            String accessCode = bureauCommonService.generateAccessCode();
            String orderId = bureauCommonService.getRandomNumberString() + mobile;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("orderId", orderId);
            headers.set("accessCode", accessCode);
            headers.set("appID", env.getProperty("crif.appId"));
            headers.set("merchantID", env.getProperty("crif.customerId"));
            String body = firstName + "||" + lastName + "|||||" + mobile + "|||||" + pancard + "|||||||||||||||||||||||" + env.getProperty("crif.customerId") + "|BBC_CONSUMER_SCORE#85#2.0|Y|";
            request = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(Objects.requireNonNull(env.getProperty("crif.stage1.url")), request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), request, e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                logger.info("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), request, null);
            }
            logger.error("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            logger.error("Exception in crif stage1 api", e);
            return null;
        }
    }

    public String crifStage2(Long mobile, String orderId, String reportId, String redirectUrl, boolean stage3, String userAns) throws BureauClientException {
        HttpEntity<String> request = null;
        try {
            String stage = stage3 ? "stage3" : "stage2";
            logger.info("Calling CRIF " + stage + " api for mobile:{} with orderId:{}", mobile, orderId);
            String accessCode = bureauCommonService.generateAccessCode();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            if (!stage3) {
                headers.set("requestType", "Authorization");
            }
            headers.set("accessCode", accessCode);
            headers.set("appID", env.getProperty("crif.appId"));
            headers.set("merchantID", env.getProperty("crif.customerId"));
            headers.set("orderId", orderId);
            headers.set("reportId", reportId);
            String body = orderId + "|" + reportId + "|" + accessCode + "|" + "https://cir.crifhighmark.com/Inquiry/B2B/secureService.action" + "|N|N|N|" + userAns;
            request = new HttpEntity<>(body, headers);
            return restTemplate.postForObject(Objects.requireNonNull(env.getProperty("crif.stage2.url")), request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.info("Client/Server Exception while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e.getStatusCode().value(), request, e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                logger.info("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
                throw new BureauClientException(e.getLocalizedMessage(), BureauStatusCode.READ_TIME_OUT.getCode(), request, null);
            }
            logger.error("ResourceAccessException while experian refresh api response : {}, {}", e.getMessage(), e);
            throw new BureauClientException(e.getLocalizedMessage(), e);
        } catch (Exception e) {
            logger.error("Exception in crif stage2 api", e);
            return null;
        }
    }

    public HistoricalBureauDataDto getHistoricalBureauData(String panCard) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("pancard", panCard);
            String url = baseUrl + BureauConstants.EXPERIAN_HIT_ID + "?pancard=" + panCard;
            HttpHeaders headers = new HttpHeaders();
            headers.set("clientName", clientName);
            String secret = commonUtils.getInternalSecret(clientName);
            Map<String, String> map = new HashMap<>();
            map.put("pancard", panCard);

            String payload = hmacCalculator.getPayload(map);
            headers.set("hash", hmacCalculator.calculateHmac(payload, secret));
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity httpEntity = new HttpEntity(headers);
            logger.info("request url:{} and headers:{}", url, headers);
            ResponseEntity<HistoricalBureauDataDto> responseBody = restTemplate.exchange(url, HttpMethod.GET, httpEntity, HistoricalBureauDataDto.class);
            logger.info("data from experian table: {} ", Objects.requireNonNull(responseBody));
            return responseBody.getBody();
        } catch (Exception e) {
            logger.error("Error occured while fetching data from api endpoint with pancard:{}", e);
        }
        return null;
    }

    public JsonNode parseResponse(String rawResponse) throws BureauParsingException {
        try {
            return mapper.readTree(rawResponse);
        } catch (Exception e) {
            logger.error("Error parsing stage 1 response : {}, {}", e.getMessage(), e);
            throw new BureauParsingException(e.getLocalizedMessage(), e);
        }
    }

    public JsonNode parseXmlResponse(String rawResponse) throws BureauParsingException {
        try {
            JSONObject jsonObject = XML.toJSONObject(rawResponse);
            return mapper.readTree(jsonObject.toString());
        } catch (Exception e) {
            logger.error("Error parsing stage 1 response : {}, {}", e.getMessage(), e);
            throw new BureauParsingException(e.getLocalizedMessage(), e);
        }
    }
}
