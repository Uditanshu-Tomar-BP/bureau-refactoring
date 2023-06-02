package com.bharatpe.lending.bureaurefactoring.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExperianDetailsDTO {
    @JsonProperty("first_name")
    private String firstName;
    @JsonProperty("last_name")
    private String lastName;
    private Long mobile;
    @JsonProperty("pancard")
    private String panCard;
    @JsonProperty("bureau_type")
    private String bureauType;
    private String source;
    @JsonProperty("stage_one_hit_id")
    private String stageOneHitId;
    @JsonProperty("stage_two_hit_id")
    private String stageTwoHitId;
    @JsonProperty("mapped_mobile")
    private Long mappedMobile;

  //  HistoricalBureauDataDto bureauHitIdResponseDTO;

//    public HistoricalBureauDataDto getBureauHitIdResponseDTO() {
//        return bureauHitIdResponseDTO;
//    }

//    public void setBureauHitIdResponseDTO(HistoricalBureauDataDto bureauHitIdResponseDTO) {
//        this.bureauHitIdResponseDTO = bureauHitIdResponseDTO;
//    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Long getMobile() {
        return mobile;
    }

    public void setMobile(Long mobile) {
        this.mobile = mobile;
    }

    public String getPanCard() {
        return panCard;
    }

    public void setPanCard(String panCard) {
        this.panCard = panCard;
    }

    public String getBureauType() {
        return bureauType;
    }

    public void setBureauType(String bureauType) {
        this.bureauType = bureauType;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "ExperianDetailsDto{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", mobile=" + mobile +
                ", panCard='" + panCard + '\'' +
                ", bureauType='" + bureauType + '\'' +
                ", source='" + source + '\'' +
                ", stageOneHitId='" + stageOneHitId + '\'' +
                ", stageTwoHitId='" + stageTwoHitId + '\'' +
                ", mappedMobile=" + mappedMobile +
              //  ", bureauHitIdResponseDTO=" +
                //bureauHitIdResponseDTO +
                '}';
    }
}
