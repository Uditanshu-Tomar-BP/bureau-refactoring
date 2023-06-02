package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
@Service
public class BureauCommonService {
    @Autowired
    BureauAPIGatewayService bureauAPIGatewayService;

    @Autowired
    Environment env;

    public long getDateDiffInDays(Date startTime, Date endTime) {
        long diff = endTime.getTime() - startTime.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }

    public HttpEntity<String> crifrequest(String firstName, String lastName, Long mobile, String panCard) {
        String accessCode = generateAccessCode();
        String orderId = getRandomNumberString() + mobile;
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.TEXT_PLAIN);
        requestHeaders.set("orderId", orderId);
        requestHeaders.set("accessCode", accessCode);
        requestHeaders.set("appID", env.getProperty("crif.appId"));
        requestHeaders.set("merchantID", env.getProperty("crif.customerId"));
        String requestBody = firstName + "||" + lastName + "|||||" + mobile + "|||||" + panCard + "|||||||||||||||||||||||" + env.getProperty("crif.customerId") + "|BBC_CONSUMER_SCORE#85#2.0|Y|";
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    public HttpEntity<MultiValueMap<String, Object>> experianHitRequest(String hitId) {
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("clientName", BureauConstants.CLIENT_NAME);
        requestBody.add("hitId", hitId);
        return new HttpEntity<>(requestBody);
    }

    public HttpEntity<MultiValueMap<String, Object>> experianRequest(String firstName, String lastName, String mobile,
                                                                     String panCard, Long mappedMobile) {
        if (mobile.length() > 10) {
            mobile = mobile.substring(2);//remove 91
        }
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        if(!Objects.isNull(mappedMobile)){
            bureauAPIGatewayService.setExperianApiParams(requestBody, firstName, lastName, String.valueOf(mappedMobile), panCard);
            return new HttpEntity<>(requestBody, requestHeaders);
        }
        bureauAPIGatewayService.setExperianApiParams(requestBody, firstName, lastName, mobile, panCard);
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    public HttpEntity<MultiValueMap<String, Object>> experianAuthenticateMaskedMobileApiRequest(String stageOneHitId, String stageTwoHitId, String mobile) {
        if (mobile.length() > 10) {
            mobile = mobile.substring(2);//remove 91
        }
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        bureauAPIGatewayService.setExperianAuthenticatedMaskedMobileApiParams(requestBody, stageOneHitId, stageTwoHitId, mobile);
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    public HttpEntity<MultiValueMap<String, Object>> experianRequest(String stgOneHitId, String stgTwoHitId) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, Object> requestBody = new LinkedMultiValueMap<>();
        bureauAPIGatewayService.setExperianApiParams(requestBody, stgOneHitId, stgTwoHitId);
        return new HttpEntity<>(requestBody, requestHeaders);
    }

    public boolean isValidReport(String panCard, String phoneNumber, JsonNode response) {

        boolean isPanMatched = true;
        boolean isPhoneChecked = true;

        if (response != null) {
            JsonNode personalData = response.get(BureauConstants.REPORT_HEADER)
                    .get(BureauConstants.PERSONAL_VARIATIONS);
            if (personalData == null || personalData.toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            if (personalData.get(BureauConstants.PAN_VARIATIONS) == null
                    || personalData.get(BureauConstants.PAN_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            if (personalData.get(BureauConstants.PHONE_VARIATIONS) == null
                    || personalData.get(BureauConstants.PHONE_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            List<JsonNode> panVariations = jsonNodeArrayUtil(personalData.get(BureauConstants.PAN_VARIATIONS).get(BureauConstants.VARIATION));
            List<JsonNode> phoneVariations = jsonNodeArrayUtil(personalData.get(BureauConstants.PHONE_VARIATIONS).get(BureauConstants.VARIATION));
            if (phoneNumber.length() > 10) {
                phoneNumber = phoneNumber.substring(2);// remove 91
            }

            isPanMatched = panVariations.size() == 0;
            isPhoneChecked = panVariations.size() == 0;

            for (JsonNode pan : panVariations) {
                isPanMatched = pan.get("VALUE").asText().equalsIgnoreCase(panCard);
                if (isPanMatched) break;
            }
            for (JsonNode phone : phoneVariations) {
                isPhoneChecked = (phone.get("VALUE").asText().equalsIgnoreCase(phoneNumber) || phone.get("VALUE").asText().equalsIgnoreCase("91" + phoneNumber));
                if (isPhoneChecked) break;
            }
        }
        return isPanMatched && isPhoneChecked;
    }


    public String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999);
        return String.format("%03d", number);
    }


    public String generateAccessCode() {
        String value = env.getProperty("crif.userId") + "|" + env.getProperty("crif.customerId") + "|BBC_CONSUMER_SCORE#85#2.0|" + env.getProperty("crif.password") + "|" + new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date());
        return Base64.getEncoder().encodeToString(value.getBytes());
    }


    public static List<JsonNode> jsonNodeArrayUtil(JsonNode nodeData) {
        List<JsonNode> resp = new ArrayList<>();
        if (nodeData != null && !nodeData.asText().equals("\"\"")) {
            if (nodeData.isObject()) {
                resp.add(nodeData);
            } else {
                for (JsonNode node : nodeData) {
                    resp.add(node);
                }
            }
        }
        return resp;
    }
}
