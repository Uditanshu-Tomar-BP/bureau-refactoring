package com.bharatpe.lending.bureaurefactoring.dto;

import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BureauAuditDataDTO {
    public String pancard;
    public String mobile;
    public String bureau_type;
    public JsonNode bureau_response;
    public String created_at;
    public String updated_at;
    public BureauStatusEnum status;
    public String rawResponse;
    public String hitId;
}
