package com.bharatpe.lending.bureaurefactoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
@ToString
public class NfiCalculationDetailsDto {
    private String pancard;
    private String mobile;
    private String firstName;
    private String lastName;
    private String hit_id;
    private Double income = 0.0;
    private Double incomePostPe = 0.0;
    private Double debt = 0.0;
    private Double nfi;
    private Double nfiPostpe;
    private List<DebtAndIncomeDto> tradelines;
}
