package com.bharatpe.lending.bureaurefactoring.dto;

import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BureauRequestResponseDTO {
    public String pancard;
    public String mobile;
    public String bureau_type;
    public Object bureau_request;
    public Object bureau_response;
    public BureauStatusEnum status;
    public Integer statusCode; //-1 in case of timeout
    public String responseBody;
    public String stage;
    public String stageCode;
    public String updated_at;
    public String created_at;
    public String hitId;
    public String source;
}
