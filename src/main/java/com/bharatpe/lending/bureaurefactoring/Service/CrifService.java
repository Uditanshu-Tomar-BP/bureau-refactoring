package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.bharatpe.lending.bureaurefactoring.constants.CrifConstants;
import com.bharatpe.lending.bureaurefactoring.dto.BureauDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauRequestResponseDTO;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusCode;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.bharatpe.lending.bureaurefactoring.exception.BureauClientException;
import com.bharatpe.lending.bureaurefactoring.exception.BureauParsingException;
import com.bharatpe.lending.bureaurefactoring.repository.MongoDataTemplate;
import com.bharatpe.lending.bureaurefactoring.utils.CommonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
@Service
public class CrifService {
    Logger logger = LoggerFactory.getLogger(CrifService.class);

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

    public BureauDataDTO getCrif(String firstName, String lastName, Long mobile, String panCard, Long days, String source) {

        String hitId = null;
        String LastBureauDate;
        String created_at = null;
        String bureauType = BureauConstants.BUREAU_TYPE_2;
        String currentStage = null;
        String currentStageResponseCode = null;
        String currentStageRawResponse = null;
        JsonNode bureauResponse = null;
        String currentBureauDate = null;
        Date previousBureauDate = null;
        String mobileBureau = null;
        HttpEntity<String> bureauRequest = null;

        try {
            logger.info("Fetching crif bureau data for mobile number: {}", mobile);
            JsonNode responseData = mongoDataTemplate.getDataV2(mobile);
            mobileBureau = mobile.toString() + "_" + bureauType;
            LocalDateTime currentDateTime = LocalDateTime.now();
            currentBureauDate = dateTimeFormatter.format(currentDateTime);
            if (responseData != null) {
                LastBureauDate = responseData.get("updated_at").asText();
                created_at = responseData.get("created_at").asText();
            } else {
                LastBureauDate = currentBureauDate;
                created_at = currentBureauDate;
            }

            previousBureauDate = simpleDateFormat.parse(LastBureauDate);
            Date latestBureauDate = simpleDateFormat.parse(currentBureauDate);
            long differenceInDays = bureauCommonService.getDateDiffInDays(previousBureauDate, latestBureauDate);
            logger.info("Gap between Bureau responses : {} ", differenceInDays);
            if (responseData != null && Objects.nonNull(responseData.get("bureau_response")) && Objects.nonNull(responseData.get("bureau_response").get("hit_id"))) {
                hitId = responseData.get("bureau_response").get("hit_id").asText();
            }

            logger.info("response data:{}", responseData);
            boolean panMatched = panCard != null && responseData != null && panCard.equalsIgnoreCase(responseData.get("pancard").asText());
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

            List<BureauRequestResponseDTO> responseDTOS = mongoDataTemplate.getRequestResponse(mobile.toString(), Bureau.CRIF, CrifConstants.STAGE_2, 1);
            logger.info("request responses from mongo: {}", responseDTOS);

            // if earlier did not get bureau data from crif then pull its data after 7 days only
            if (!ObjectUtils.isEmpty(responseDTOS) && Objects.nonNull(responseDTOS.get(0)) && (simpleDateFormat.parse(responseDTOS.get(0).getUpdated_at()).after(BureauService.getDatePlusDays(new Date(), -7)))) {
                return BureauDataDTO.builder().build();
            }
            else {
                // don't call bureau if the PAN is not personal
                if (!ObjectUtils.isEmpty(panCard) && !CommonUtil.isPersonalPan(panCard)){
                    logger.info("pan not personal for mobile : {} , pan : {} ", mobile, panCard);
                    return null;
                }

                bureauRequest = bureauCommonService.crifrequest(firstName, lastName, mobile, panCard);
                logger.info("Bureau request to fetch response : {} ", bureauRequest);
                currentStage = CrifConstants.STAGE_1;
                currentStageRawResponse = bureauAPIGatewayService.crifStage1(firstName, lastName, panCard, mobile.toString());
                JsonNode stage1Response = bureauAPIGatewayService.parseResponse(currentStageRawResponse);
                currentStageResponseCode = Objects.nonNull(stage1Response) ? stage1Response.get("status").asText() : "";

                //todo: check if request needs to be captured on stage basis
                //stage 1 audit
                bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, panCard, stage1Response, bureauType, bureauRequest,
                        currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(), currentStageRawResponse, currentStage, currentStageResponseCode, source));

                if (stage1Response != null && stage1Response.get("status") != null && currentStageResponseCode.equals("S06")) {
                    logger.info("Crif stage1 success for mobile:{}", mobile);
                    currentStage = CrifConstants.STAGE_2;
                    currentStageRawResponse = bureauAPIGatewayService.crifStage2(mobile, stage1Response.get("orderId").asText(), stage1Response.get("reportId").asText(), stage1Response.get("redirectURL").asText(), false, "");
                    JsonNode stage2Response = bureauAPIGatewayService.parseResponse(currentStageRawResponse);
                    currentStageResponseCode = Objects.nonNull(stage2Response) ? stage2Response.get("status").asText() : "";

                    //stage 2 audit
                    bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, panCard, stage2Response, bureauType, bureauRequest,
                            currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(), currentStageRawResponse, currentStage, currentStageResponseCode, source));

                    if (stage2Response != null && stage2Response.get("status") != null && (currentStageResponseCode.equals("S10") || currentStageResponseCode.equals("S01"))) {
                        logger.info("Crif stage2 success for mobile:{} and currentStageResponseCode: {}", mobile, currentStageResponseCode);
                        currentStage = CrifConstants.STAGE_3;
                        currentStageRawResponse = bureauAPIGatewayService.crifStage2(mobile, stage1Response.get("orderId").asText(), stage1Response.get("reportId").asText(), stage1Response.get("redirectURL").asText(), true, "");
                        JsonNode stage3Response = bureauAPIGatewayService.parseXmlResponse(currentStageRawResponse);

                        //stage 3 audit
                        bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, panCard, stage3Response, bureauType, bureauRequest,
                                currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(), currentStageRawResponse, currentStage, currentStageResponseCode, source));

                        if (stage3Response != null) {
                            logger.info("Found crif report for mobile:{}", mobile);
                            if (bureauCommonService.isValidReport(panCard, mobile.toString(), stage3Response)) {
                                bureauResponse = stage3Response;
                            } else {
                                logger.error("Invalid crif report for mobile:{}", mobile);
                                throw new BureauParsingException("Invalid report", BureauStatusCode.INVALID_REPORT.getCode());
                            }
                        }
                    } else {
                        throw new BureauParsingException("Security Question required", BureauStatusCode.SECURITY_QUESTION.getCode());
                    }
                }
                logger.info("Bureau response : {} ", bureauResponse);
                logger.info("Bureau type : {}", bureauType);
                if (panCard == null && bureauResponse != null) {
                    panCard = fetchPancard(bureauResponse, mobile.toString());
                }
                return bureauResponseService.getResponse(mobile, panCard, firstName, lastName, bureauResponse,
                        bureauType, bureauRequest, currentBureauDate, previousBureauDate, created_at, null, mobileBureau,
                        "After report Validation", source, null);
            }
        }
        catch (BureauClientException e) {
            logger.error("BureauClientException in experian for mobile:{}, {}, {}", mobile, e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, panCard, bureauResponse, bureauType, e.getRequest(),
                    currentBureauDate, BureauStatusEnum.FAILED, e.getHttpStatus(), e.getResponseBody(), currentStage, currentStageResponseCode, source));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, currentStageRawResponse, null));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, firstName, lastName,
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, created_at, null, mobileBureau,
                    BureauStatusEnum.FAILED, e.getHttpStatus(), null));
        } catch (BureauParsingException e) {
            logger.error("Exception while parsing experian refresh api response for mobile : {}, {} , {}", mobile, e.getMessage(), e);
            bureauResponseService.auditRequestResponse(bureauResponseService.getBureauRequestResponseDTO(mobile, source, panCard,
                    bureauResponse, bureauType, bureauRequest, currentBureauDate, BureauStatusEnum.FAILED, HttpStatus.OK.value(),
                    currentStageRawResponse, null, true));

            bureauResponseService.auditBureauData(bureauResponseService.getBureauAuditDataDTO(mobile, panCard, bureauResponse,
                    bureauType, currentBureauDate, BureauStatusEnum.FAILED, currentStageRawResponse, null));

            bureauResponseService.updateBureauData(bureauResponseService.getBureauDataDTO(mobile, panCard, firstName, lastName,
                    bureauResponse, bureauType, currentBureauDate, previousBureauDate, created_at, null, mobileBureau,
                    BureauStatusEnum.FAILED, e.getHttpStatus(), null));
        } catch (Exception e) {
            logger.error("Exception in crif for mobile:{}", mobile, e);
        }
        return null;
    }

    private String fetchPancard(JsonNode bureauResponse, String mobile) {
        try {
            logger.info("fetching pancard from experian for mobile:{}", mobile);
            JsonNode personalData = bureauResponse.get(BureauConstants.REPORT_HEADER).get(BureauConstants.PERSONAL_VARIATIONS);
            if (personalData == null || personalData.toString().equalsIgnoreCase("\"\"")) {
                return null;
            }
            if (personalData.get(BureauConstants.PAN_VARIATIONS) == null || personalData.get(BureauConstants.PAN_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                return null;
            }
            List<JsonNode> panVariations = BureauCommonService.jsonNodeArrayUtil(personalData.get(BureauConstants.PAN_VARIATIONS).get(BureauConstants.VARIATION));
            for (JsonNode pan : panVariations) {
                return pan.get("VALUE").asText();
            }
        } catch (Exception e) {
            logger.error("Exception while fetching pancard from crif", e);
        }
        return null;
    }
}
