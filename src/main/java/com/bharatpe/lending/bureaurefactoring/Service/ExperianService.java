package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.bharatpe.lending.bureaurefactoring.constants.ExperianConstants;
import com.bharatpe.lending.bureaurefactoring.dto.BureauDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauRequestResponseDTO;
import com.bharatpe.lending.bureaurefactoring.dto.ExperianDetailsDTO;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusCode;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.bharatpe.lending.bureaurefactoring.exception.BureauCallMaskedApiException;
import com.bharatpe.lending.bureaurefactoring.exception.BureauClientException;
import com.bharatpe.lending.bureaurefactoring.exception.BureauParsingException;
import com.bharatpe.lending.bureaurefactoring.repository.MongoDataTemplate;
import com.bharatpe.lending.bureaurefactoring.utils.CommonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
@Service
public class ExperianService {
    Logger logger = LoggerFactory.getLogger(ExperianService.class);

    @Autowired
    BureauAPIGatewayService bureauAPIGatewayService;

    @Autowired
    BureauResponseService bureauResponseService;

//    @Autowired
//    MongetService mongetService;
    @Autowired
    MongoDataTemplate mongoDataTemplate;

    @Autowired
    BureauCommonService bureauCommonService;

    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public BureauDataDTO getExperian(String firstName, String lastName, Long mobile, String panCard, Long days, String source) throws Exception {

        String hitId = null;
        String LastBureauDate;
        String createdAt = null;
        String bureauType = BureauConstants.BUREAU_TYPE_1;
        String rawResponse = null;
        JsonNode bureauResponse = null;
        HttpEntity<MultiValueMap<String, Object>> bureauRequest = null;
        String currentBureauDate = null;
        Date previousBureauDate = null;
        String mobileBureau = null;
        Long mappedMobile = null;

        try {
            logger.info("Fetching  Experian bureau data for mobile number: {}", mobile);
            JsonNode responseData = mongoDataTemplate.getDataV2(mobile);
            mobileBureau = mobile.toString() + "_" + bureauType;
            LocalDateTime currentDateTime = LocalDateTime.now();
            currentBureauDate = dateTimeFormatter.format(currentDateTime);
            if (responseData != null) {
                LastBureauDate = responseData.get("updated_at").asText();
                createdAt = responseData.get("created_at").asText();
                mappedMobile = responseData.hasNonNull("mappedMobile") ? responseData.get("mappedMobile").asLong() : null;
            } else {
                LastBureauDate = currentBureauDate;
                createdAt = currentBureauDate;
            }
            logger.info("Previous response date : {} ", LastBureauDate);
            logger.info("Current response date : {} ", currentBureauDate);
            previousBureauDate = simpleDateFormat.parse(LastBureauDate);
            Date latestBureauDate = simpleDateFormat.parse(currentBureauDate);
            long differenceInDays = bureauCommonService.getDateDiffInDays(previousBureauDate, latestBureauDate);
            logger.info("Gap between Bureau responses : {} ", differenceInDays);
            if (responseData != null && Objects.nonNull(responseData.get("bureau_response")) && Objects.nonNull(responseData.get("bureau_response").get("hit_id"))) {
                hitId = responseData.get("bureau_response").get("hit_id").asText();
            }
            logger.info("response data:{}", responseData);

            boolean panMatched = panCard != null && responseData != null && panCard.equalsIgnoreCase(responseData.get("pancard").asText());
            String responseErrorString = responseData != null && Objects.nonNull(responseData.get("bureau_response"))
                    && Objects.nonNull(responseData.get("bureau_response").get("errorString")) ? responseData.get("bureau_response").get("errorString").asText() : null;
            boolean hasAuthError = BureauConstants.AUTHORIZED_TO_CALL_MASK_MOBILE_API.equalsIgnoreCase(responseErrorString);
            if(hasAuthError) {
                throw new BureauCallMaskedApiException("Call Experian Masked Mobile API", responseData.get("bureau_response"));
            }
            if (differenceInDays < days && responseData != null && Objects.nonNull(responseData.get("bureau_response")) && (panCard == null || panMatched)) {
                return BureauDataDTO.builder()
                        .pancard(responseData.get("pancard").asText())
                        .mobile(responseData.get("mobile").asText())
                        .bureau_type(responseData.get("bureau_type").asText())
                        .bureau_response(responseData.get("bureau_response"))
                        .updated_at(responseData.get("updated_at").asText())
                        .report_date(responseData.get("report_date").asText())
                        .created_at(responseData.get("created_at").asText())
                        .identifier(mobile.toString())
                        .hit_id(hitId)
                        .mobile_bureau(ObjectUtils.isEmpty(responseData.get("mobile_bureau")) ? null : responseData.get("mobile_bureau").asText())
                        .build();
            }
            List<BureauRequestResponseDTO> responseDTOS = mongoDataTemplate.getRequestResponse(mobile.toString(), Bureau.EXPERIAN, null,1);
            logger.info("request responses from mongo: {}", responseDTOS);

            // if earlier did not get bureau data from crif then pull its data after 7 days only
            if (!ObjectUtils.isEmpty(responseDTOS) && Objects.nonNull(responseDTOS.get(0))
                    && (simpleDateFormat.parse(responseDTOS.get(0).getUpdated_at()).after(BureauService.getDatePlusDays(new Date(), -7)))) {
                return BureauDataDTO.builder().build();

            } else if (hitId != null && mappedMobile == null && (panCard == null || panMatched)) {
                bureauRequest = bureauCommonService.experianHitRequest(hitId);
                logger.info("Experian Refresh Api request to fetch response : {} ", bureauRequest);
                rawResponse = bureauAPIGatewayService.experianRefreshApi(hitId);
                bureauResponse = bureauAPIGatewayService.parseExperianResponse(rawResponse);
                logger.info("Experian Refresh Api response : {} ", bureauResponse);

                if (panCard == null && bureauResponse != null) {
                    panCard = fetchPancard(bureauResponse, mobile);
                }
            } else {

                // don't call bureau if the PAN is not personal
                if (!ObjectUtils.isEmpty(panCard) && !CommonUtil.isPersonalPan(panCard)){
                    logger.info("pan not personal for mobile : {} , pan : {} ", mobile, panCard);
                    return null;
                }

                bureauRequest = bureauCommonService.experianRequest(firstName, lastName, String.valueOf(mobile), panCard, mappedMobile);
                rawResponse = bureauAPIGatewayService.fetchExperianDetails(bureauRequest, mobile);
                JsonNode parsedBureauResponse = bureauAPIGatewayService.parseExperianResponse(rawResponse);
                if(parsedBureauResponse !=  null ){
                    String errorString = !ObjectUtils.isEmpty(parsedBureauResponse.get("errorString")) ? parsedBureauResponse.get("errorString").asText() : null;
                    if(BureauConstants.AUTHORIZED_TO_CALL_MASK_MOBILE_API.equals(errorString)) {
                        logger.info("Got response from bureau to call Masked Mobile Api: {}", parsedBureauResponse);
                        bureauResponse = bureauAPIGatewayService.setHitId(parsedBureauResponse, rawResponse);
                        bureauResponseService.getResponse(mobile, panCard, firstName, lastName, parsedBureauResponse, bureauType,
                                bureauRequest, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau, rawResponse, source, mappedMobile );
                        throw new BureauCallMaskedApiException("Call Experian Masked Mobile API", parsedBureauResponse);
                    }
                }
                bureauResponse = bureauAPIGatewayService.setHitId(parsedBureauResponse, rawResponse);
                logger.info("Experian response : {} ", bureauResponse);
                hitId = bureauResponse != null ? bureauResponse.get("hit_id").asText() : null;
                if (panCard == null && bureauResponse != null) {
                    panCard = fetchPancard(bureauResponse, mobile);
                }
            }
            return bureauResponseService.getResponse(mobile, panCard, firstName, lastName, bureauResponse, bureauType,
                    bureauRequest, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau, rawResponse, source, mappedMobile );
        } catch (BureauCallMaskedApiException e){
            throw(e);
        } catch (BureauClientException e) {
            logger.error("BureauClientException in experian for mobile:{}, {}, {}", mobile, e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, source ,panCard,
                    bureauResponse, bureauType, bureauRequest, currentBureauDate, BureauStatusEnum.FAILED, e.getHttpStatus(),
                    e.getResponseBody(), hitId,true));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, rawResponse, hitId));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, firstName, lastName,
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau,
                    BureauStatusEnum.FAILED, e.getHttpStatus(), mappedMobile));

        } catch (BureauParsingException e) {
            logger.error("Exception while parsing experian refresh api response for mobile : {}, {} , {}", mobile, e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, source ,panCard,
                    bureauResponse, bureauType, bureauRequest, currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(),
                    rawResponse, hitId, true));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, rawResponse, hitId));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, firstName, lastName,
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau,
                    BureauStatusEnum.FAILED, BureauStatusCode.PARSE_ERROR.getCode(), mappedMobile));
        } catch (Exception e) {
            logger.error("Exception in experian for mobile:{}", mobile, e);
        }
        return null;
    }

    public BureauDataDTO getAuthenticatedExperianResponse(ExperianDetailsDTO requestDto, Long days) {

        String hitId = null;
        String LastBureauDate;
        String createdAt = null;
        String bureauType = BureauConstants.BUREAU_TYPE_1;
        String rawResponse = null;
        JsonNode bureauResponse = null;
        HttpEntity<MultiValueMap<String, Object>> bureauRequest = null;
        String currentBureauDate = null;
        Date previousBureauDate = null;
        String mobileBureau = null;
        String panCard = requestDto.getPanCard();
        Long mobile = requestDto.getMobile();

        try {
            logger.info("Fetching Experian bureau details for mobile:{} and pan:{} using mapped mobile no:{}", requestDto.getMobile(), requestDto.getPanCard(), requestDto.getMappedMobile());
            JsonNode responseData = mongoDataTemplate.getDataV2(requestDto.getMobile());
            mobileBureau = requestDto.getMobile() + "_" + bureauType;
            LocalDateTime currentDateTime = LocalDateTime.now();
            currentBureauDate = dateTimeFormatter.format(currentDateTime);
            previousBureauDate = simpleDateFormat.parse(currentBureauDate);
            createdAt = currentBureauDate;
            logger.info("Current response date : {} ", currentBureauDate);
            bureauRequest = bureauCommonService.experianAuthenticateMaskedMobileApiRequest(requestDto.getStageOneHitId(), requestDto.getStageTwoHitId(), String.valueOf(requestDto.getMappedMobile()));
            rawResponse = bureauAPIGatewayService.fetchAuthenticatedMaskedExperianDetails(bureauRequest, requestDto.getMobile());
            JsonNode parsedBureauResponse = bureauAPIGatewayService.parseExperianResponse(rawResponse);
            bureauResponse = bureauAPIGatewayService.setHitId(parsedBureauResponse, rawResponse);
            logger.info("Experian response : {} ", bureauResponse);
            hitId = bureauResponse != null && bureauResponse.hasNonNull("hit_id") ? bureauResponse.get("hit_id").asText() : null;

            if (panCard == null && bureauResponse != null) {
                panCard = fetchPancard(bureauResponse, mobile);
            }
            return bureauResponseService.getResponse(mobile, panCard, requestDto.getFirstName(), requestDto.getLastName(), bureauResponse, bureauType,
                    bureauRequest, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau, rawResponse, requestDto.getSource(), requestDto.getMappedMobile());
        } catch (BureauClientException e) {
            logger.error("BureauClientException in experian for mobile:{} and pan:{} using mapped mobile no:{}, {}, {}",
                    requestDto.getMobile(), requestDto.getPanCard(), requestDto.getMappedMobile(), e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, requestDto.getSource(), panCard,
                    bureauResponse, bureauType, bureauRequest, currentBureauDate, BureauStatusEnum.FAILED, e.getHttpStatus(),
                    e.getResponseBody(), hitId, true));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, rawResponse, hitId));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, requestDto.getFirstName(), requestDto.getLastName(),
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau,
                    BureauStatusEnum.FAILED, e.getHttpStatus(), requestDto.getMappedMobile()));

        } catch (BureauParsingException e) {
            logger.error("BureauParsingException in experian for mobile:{} and pan:{} using mapped mobile no:{}, {}, {}",
                    requestDto.getMobile(), requestDto.getPanCard(), requestDto.getMappedMobile(), e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, requestDto.getSource(), panCard,
                    bureauResponse, bureauType, bureauRequest, currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(),
                    rawResponse, hitId, true));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, rawResponse, hitId));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, requestDto.getFirstName(), requestDto.getLastName(),
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, createdAt, hitId, mobileBureau,
                    BureauStatusEnum.FAILED, BureauStatusCode.PARSE_ERROR.getCode(), requestDto.getMappedMobile()));
        } catch (Exception e) {
            logger.error("Exception in experian for mobile:{} and pan:{} using mapped mobile no:{}, {}, {}",
                    requestDto.getMobile(), requestDto.getPanCard(), requestDto.getMappedMobile(), e.getMessage(), e);
        }
        return null;
    }

    public BureauDataDTO getMaskedMobileNosFromExperian(String panCard, String stageOneHitId, String stageTwoHitId) {
        try {
            logger.info("Fetching Masked Mobile No from Experian for pancard: {}", panCard);
            HttpEntity<MultiValueMap<String, Object>> bureauRequest = bureauCommonService.experianRequest(stageOneHitId, stageTwoHitId);
            String rawResponse = bureauAPIGatewayService.fetchMaskedExperianDetails(bureauRequest);
            JsonNode maskedBureauResponse = bureauAPIGatewayService.parseExperianMaskedMobileApiResponse(rawResponse);
            return BureauDataDTO.builder().bureau_response(maskedBureauResponse).build();
        } catch (BureauClientException e) {
            logger.error("BureauClientException in experian while fetching masked mobile nos for pancard:{} , {}", panCard, e.getMessage(), e);
        } catch (BureauParsingException e) {
            logger.error("Exception while parsing experian masked mobile api response for pancard:{} : {}, {}", panCard, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Exception in Masked Mobile API for Experian", e);
        }
        return null;
    }

    private String fetchPancard(JsonNode bureauResponse, Long mobile) {
        try {
            logger.info("fetching pancard from experian for mobile:{}", mobile);
            String pancard = null;
            JsonNode currentApplicationDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CURRENT_APPLICATION).get(ExperianConstants.CURRENT_APPLICATION_DETAILS);
            if (currentApplicationDetails != null && currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS) != null) {
                pancard = currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS).get(ExperianConstants.INCOME_TAX_PAN).asText();
            }
            if (!StringUtils.isEmpty(pancard) && !"null".equalsIgnoreCase(pancard)) {
                logger.info("Pancard:{} fetched for mobile:{}", pancard, mobile);
                return pancard;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while fetching pancard from experian for mobile:{}", mobile, e);
        }
        return null;
    }
}
