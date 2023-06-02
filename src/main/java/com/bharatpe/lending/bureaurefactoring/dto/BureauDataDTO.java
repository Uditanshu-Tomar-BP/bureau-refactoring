package com.bharatpe.lending.bureaurefactoring.dto;

import com.bharatpe.lending.bureaurefactoring.enums.BureauStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BureauDataDTO {
    public String pancard;
    public String mobile;
    public String firstName;
    public String lastName;
    public String bureau_type;
    public JsonNode bureau_response;
    public String created_at;
    public String report_date;
    public String updated_at;
    public String identifier;
    public String hit_id;
    public String mobile_bureau;
    public BureauStatusEnum status;
    public Integer statusCode;
    public String source;
    public String mappedMobile;
    public ConsentDetails consentDetails;
    public BureauResponseDTO.BureauVariables bureauVariables;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConsentDetails {
        public String mobileId;
        public String latitude;
        public String longitude;
        public String ip;
        public String consentDate;
        public Boolean consent;
      //  @JsonDeserialize(using = ReportDateDeserializer.class)
        public Long consentDateEpoch;
    }
}
