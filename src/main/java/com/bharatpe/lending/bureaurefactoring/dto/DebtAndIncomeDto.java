package com.bharatpe.lending.bureaurefactoring.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DebtAndIncomeDto {
    private String loanCode;

    private Double loanAmount;

    private Double emiProportion;

    private Double dbiProportion;

    private Double dbiProportionPostPe;

    private Boolean toBeConsidered = false;

    private String openDate;

    private String closeDate;

    private Double currentBalance;

    private Double debt = 0.0;

    private Double income = 0.0;

    private Double incomePostPe = 0.0;
}
