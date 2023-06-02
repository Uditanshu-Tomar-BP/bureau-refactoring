package com.bharatpe.lending.bureaurefactoring.Service;

import com.bharatpe.lending.bureaurefactoring.dto.KafkaAudit;
import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
@Service
@Slf4j
public class AuditService {
    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;


    public void auditNfiCalculationDetails(NfiCalculationDetailsDto dto, String entityName) {
        try {
            KafkaAudit<NfiCalculationDetailsDto> kafkaAudit = new KafkaAudit<>("easy_loan", "bureau", entityName, null);
            kafkaAudit.setData(dto);
            pushKafkaAudit(kafkaAudit);
            log.info("successfully audit nfi calculation details data: {} ", dto);
        } catch (Exception e) {
            log.error("Exception occurred while auditing nfiDetails: {}, {}", dto, e);
        }
    }

    public void pushKafkaAudit(KafkaAudit kafkaAudit) {
        try {
            log.info("pushing kafka event for {}", kafkaAudit);
            kafkaTemplate.send("easyloan_audit_data", kafkaAudit);
        } catch (Exception e) {
            log.error("error while sending audit data {} {}", kafkaAudit, Arrays.asList(e.getStackTrace()));
        }
    }
}
