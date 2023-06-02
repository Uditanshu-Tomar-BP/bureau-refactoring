package com.bharatpe.lending.bureaurefactoring.nfi;

import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import com.fasterxml.jackson.databind.JsonNode;

public interface NfiCalculator {
    public NfiCalculationDetailsDto getDebtAndIncomeDetails(JsonNode loanDetails) ;

    public default Double getNetFreeIncome(Double estimatedIncome, Double debt) {
        Double expenses = 0.20 * estimatedIncome;
        double totalExpenses = expenses + debt;
        return estimatedIncome - totalExpenses;
    }
}
