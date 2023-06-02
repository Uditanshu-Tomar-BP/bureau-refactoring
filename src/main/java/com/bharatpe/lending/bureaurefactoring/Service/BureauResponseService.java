package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.common.service.MongoPublisher;
import com.bharatpe.lending.bureaurefactoring.constants.BureauConstants;
import com.bharatpe.lending.bureaurefactoring.dto.BureauAuditDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauDataDTO;
import com.bharatpe.lending.bureaurefactoring.dto.BureauRequestResponseDTO;
import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
@Service
@Slf4j
public class BureauResponseService {
    @Autowired
    MongoUpdatePublisher mongoUpdatePublisher;

    @Autowired
    MongoPublisher mongoPublisher;

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    BureauDataDTO getResponse(Long mobile, String panCard, String firstName, String lastName, JsonNode bureauResponse,
                              String bureauType, Object bureauRequest, String currentBureauDate, Date previousBureauDate,
                              String created_at, String hitId, String mobileBureau, String rawResponse, String source, Long mappedMobile) {

        log.info("getting {} response for mobile number: {}", bureauType, mobile);
        auditBureauData(getBureauAuditDataDTO(mobile, panCard, bureauResponse, bureauType, currentBureauDate,
                BureauStatusEnum.SUCCESS, rawResponse, hitId));

        boolean addRawResponse = bureauResponse == null;

        auditRequestResponse(getBureauRequestResponseDTO(mobile, source,panCard, bureauResponse, bureauType, bureauRequest,
                currentBureauDate, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(), rawResponse, hitId, addRawResponse));

        BureauDataDTO bureauDataDTO = getBureauDataDTO(mobile, panCard, firstName, lastName, bureauResponse, bureauType, currentBureauDate,
                previousBureauDate, created_at, hitId, mobileBureau, BureauStatusEnum.SUCCESS, HttpStatus.OK.value(), mappedMobile);
        updateBureauData(bureauDataDTO);

        return bureauDataDTO;
    }

    public void updateBureauData(BureauDataDTO bureauDataDTO) {
        mongoUpdatePublisher.publish("Bureau", "bureau_data", bureauDataDTO.getMobile(),
                new ArrayList<BureauDataDTO>() {{
                    add(bureauDataDTO);
                }}, BureauConstants.BUREAU_DATA_SHARD_KEY, bureauDataDTO.getMobile());
        log.info("Bureau data successfully published for mobile:{}", bureauDataDTO.getMobile());
    }

    public void auditRequestResponse(BureauRequestResponseDTO bureauRequestResponseDTO) {
        mongoPublisher.publish("Bureau", "bureau_request_response",
                bureauRequestResponseDTO.getMobile(), new ArrayList<BureauRequestResponseDTO>() {{
                    add(bureauRequestResponseDTO);
                }});
    }

    public void auditBureauData(BureauAuditDataDTO bureauAuditDataDTO) {
        mongoPublisher.publish("Bureau", "bureau_audit_data", bureauAuditDataDTO.getMobile(),
                new ArrayList<BureauAuditDataDTO>() {{
                    add(bureauAuditDataDTO);
                }});
    }

    public BureauDataDTO getBureauDataDTO(Long mobile, String panCard, String firstName, String lastName, JsonNode bureauResponse,
                                          String bureauType, String currentBureauDate, Date previousBureauDate, String created_at,
                                          String hitId, String mobileBureau, BureauStatusEnum status, Integer statusCode, Long mappedMobile) {
        return BureauDataDTO.builder()
                .pancard(panCard)
                .mobile(mobile.toString())
                .firstName(firstName)
                .lastName(lastName)
                .mappedMobile(mappedMobile != null ? mappedMobile.toString() : null)
                .bureau_type(bureauType)
                .bureau_response(bureauResponse)
                .updated_at(currentBureauDate)
                .report_date(simpleDateFormat.format(previousBureauDate))
                .created_at(created_at)
                .identifier(mobile.toString())
                .hit_id(hitId)
                .mobile_bureau(mobileBureau)
                .status(status)
                .statusCode(statusCode)
                .build();
    }

    public BureauRequestResponseDTO getBureauRequestResponseDTO(Long mobile, String source ,String panCard, JsonNode bureauResponse,
                                                                String bureauType, Object bureauRequest, String currentBureauDate,
                                                                BureauStatusEnum bureauStatus, Integer httpStatus, String responseBody, String hitId,
                                                                boolean addResponseBody) {
        return BureauRequestResponseDTO.builder()
                .pancard(panCard)
                .mobile(mobile.toString())
                .source(source)
                .bureau_type(bureauType)
                .bureau_request(bureauRequest)
                .bureau_response(getBureauAuditDataDTO(mobile, panCard, bureauResponse, bureauType, currentBureauDate, bureauStatus, responseBody, hitId))
                .statusCode(httpStatus)
                .hitId(hitId)
                .responseBody(addResponseBody ? responseBody: null)
                .updated_at(currentBureauDate)
                .created_at(currentBureauDate)
                .status(bureauStatus)
                .build();
    }

    public BureauRequestResponseDTO getBureauRequestResponseDTO(Long mobile, String panCard, JsonNode bureauResponse,
                                                                String bureauType, Object bureauRequest, String currentBureauDate,
                                                                BureauStatusEnum bureauStatus, Integer httpStatus, String responseBody,
                                                                String stage, String stageCode, String source) {
        BureauRequestResponseDTO bureauRequestResponseDTO = getBureauRequestResponseDTO(mobile, source ,panCard, bureauResponse,
                bureauType, bureauRequest, currentBureauDate, bureauStatus, httpStatus, responseBody, null, true);
        bureauRequestResponseDTO.setStage(stage);
        bureauRequestResponseDTO.setStageCode(stageCode);
        return bureauRequestResponseDTO;
    }

    public BureauAuditDataDTO getBureauAuditDataDTO(Long mobile, String panCard, JsonNode bureauResponse,
                                                    String bureauType, String currentBureauDate,
                                                    BureauStatusEnum status, String rawResponse, String hitId) {
        return BureauAuditDataDTO.builder()
                .pancard(panCard)
                .mobile(mobile.toString())
                .bureau_type(bureauType)
                .bureau_response(bureauResponse)
                .created_at(currentBureauDate)
                .updated_at(currentBureauDate)
                .hitId(hitId)
//          .rawResponse(rawResponse)
                .status(status)
                .build();
    }
}
