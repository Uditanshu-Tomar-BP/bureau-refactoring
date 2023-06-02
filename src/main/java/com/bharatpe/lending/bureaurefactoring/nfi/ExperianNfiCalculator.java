package com.bharatpe.lending.bureaurefactoring.nfi;

import com.bharatpe.lending.bureaurefactoring.constants.Constant;
import com.bharatpe.lending.bureaurefactoring.constants.ExperianConstants;
import com.bharatpe.lending.bureaurefactoring.dto.DebtAndIncomeDto;
import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
@Slf4j
public class ExperianNfiCalculator {
    static List<Integer> activeStatusList = Arrays.asList(11, 21, 22, 23, 24, 25, 71, 78, 80, 82, 83, 84);

    SimpleDateFormat dateFormat = new SimpleDateFormat(ExperianConstants.DATE_FORMAT);

    public NfiCalculationDetailsDto getDebtAndIncomeDetails(JsonNode loanDetails) {
        NfiCalculationDetailsDto nfiCalculationDetailsDto = new NfiCalculationDetailsDto();
        double debt = 0D;
        double totalIncome = 0d;
        double totalIncomePostpe = 0d;
        double maxCreditCardIncome = 0d;
        double maxCreditCardIncomePostpe = 0d;
        ArrayList<DebtAndIncomeDto> tradelines = new ArrayList<>();
        if (!loanDetails.isArray()) {
            DebtAndIncomeDto debtAndIncomeDto = getDebtAndIncomeOfALoan(loanDetails);
            tradelines.add(debtAndIncomeDto);
            nfiCalculationDetailsDto.setTradelines(tradelines);
            nfiCalculationDetailsDto.setDebt(debtAndIncomeDto.getDebt());
            nfiCalculationDetailsDto.setIncome(debtAndIncomeDto.getIncome());
            nfiCalculationDetailsDto.setIncomePostPe(debtAndIncomeDto.getIncomePostPe());
            if ("CC".equals(debtAndIncomeDto.getLoanCode())) {
                nfiCalculationDetailsDto.setIncomePostPe(Math.min(debtAndIncomeDto.getIncomePostPe(), Constant.THRESHOLD_CREDIT_CARD_INCOME_POSTPE));
            }
            return nfiCalculationDetailsDto;
        }
        for (JsonNode loan : loanDetails) {
            log.info("loan: {}", loan);
            DebtAndIncomeDto debtAndIncomeDto = getDebtAndIncomeOfALoan(loan);
            log.info("debtAndIncomeDto: {}", debtAndIncomeDto);
            tradelines.add(debtAndIncomeDto);
            if (ObjectUtils.isEmpty(debtAndIncomeDto.getDebt())) {
                continue;
            }
            debt += debtAndIncomeDto.getDebt();
            //Only need to consider maximum credit_loan income into total_estimated_income
            if ("CC".equals(debtAndIncomeDto.getLoanCode())) {
                if (!ObjectUtils.isEmpty(debtAndIncomeDto.getIncome()) && maxCreditCardIncome < debtAndIncomeDto.getIncome())
                    maxCreditCardIncome = debtAndIncomeDto.getIncome();
                if (!ObjectUtils.isEmpty(debtAndIncomeDto.getIncomePostPe()) && maxCreditCardIncomePostpe < debtAndIncomeDto.getIncomePostPe())
                    maxCreditCardIncomePostpe = debtAndIncomeDto.getIncomePostPe();
                continue;
            }
            if (!ObjectUtils.isEmpty(debtAndIncomeDto.getIncome())) {
                totalIncome += debtAndIncomeDto.getIncome();
            }
            if (!ObjectUtils.isEmpty(debtAndIncomeDto.getIncomePostPe())) {
                totalIncomePostpe += debtAndIncomeDto.getIncomePostPe();
            }
        }
        totalIncome += maxCreditCardIncome;
        totalIncomePostpe += Math.min(maxCreditCardIncomePostpe, Constant.THRESHOLD_CREDIT_CARD_INCOME_POSTPE);
        nfiCalculationDetailsDto.setTradelines(tradelines);
        nfiCalculationDetailsDto.setDebt(debt);
        nfiCalculationDetailsDto.setIncome(totalIncome);
        nfiCalculationDetailsDto.setIncomePostPe(totalIncomePostpe);
        return nfiCalculationDetailsDto;
    }


    private DebtAndIncomeDto getDebtAndIncomeOfALoan(JsonNode loan) {
        DebtAndIncomeDto debtAndIncome = new DebtAndIncomeDto();
        int loanTypeNumber = loan.get(ExperianConstants.ACCT_TYPE).asInt();
        String loanCode = getLoanType(loanTypeNumber);
        double loanAmount = getLoanAmount(loan);
        boolean isLoanClosed = isLoanClosed(loan);

        debtAndIncome.setLoanCode(loanCode);
        debtAndIncome.setLoanAmount(loanAmount);
        debtAndIncome.setCloseDate(String.valueOf(loan.get(ExperianConstants.DATE_CLOSED)));

        if (loanAmount < 10000 || isLoanClosed)
            return debtAndIncome;

        Double currentBalance = getCurrentBalance(loan);
        debtAndIncome.setCurrentBalance(currentBalance);
        //NON credit card Current balance<5000 to be ignored in calculation
        if (!"CC".equals(loanCode) && currentBalance < 5000)
            return debtAndIncome;

        debtAndIncome.setToBeConsidered(Constant.LOAN_CONSIDERED.get(loanCode));
        if (!Constant.LOAN_CONSIDERED.get(loanCode))
            return debtAndIncome;

        debtAndIncome.setEmiProportion(Constant.EMI_PROPORTION.get(loanCode));
        double debt = loanAmount * Constant.EMI_PROPORTION.get(loanCode);
        debtAndIncome.setDebt(debt);
        debtAndIncome.setOpenDate(String.valueOf(loan.get(ExperianConstants.OPEN_DATE)));

        if (!loanToBeConsideredForIncome(loan, loanCode))
            return debtAndIncome;
        double dbi = Constant.DBI_PROPORTION.get(loanCode);
        double dbi_pp = Constant.DBI_PROPORTION_PP.get(loanCode);
        debtAndIncome.setDbiProportion(dbi);
        debtAndIncome.setDbiProportionPostPe(dbi_pp);
        if (dbi > 0) {
            Double income = debt / dbi;
            debtAndIncome.setIncome(income);
        }
        if (dbi_pp > 0) {
            double incomePostPe = "CC".equals(loanCode) ? loanAmount * Constant.CREDIT_CARD_INCOME_POSTPE_MULTIPLIER : debt / dbi_pp;
//            if ("CC".equals(loanCode)) {
//                incomePostPe = loanAmount * Constant.CREDIT_CARD_INCOME_POSTPE_MULTIPLIER;
//            }else {
//                incomePostPe = debt / dbi_pp;
//            }
            debtAndIncome.setIncomePostPe(incomePostPe);
        }
        return debtAndIncome;
    }

    /**
     * Credit Card Tradelines opened in the last 90 days AND Non Credit card Tradelines opened in the last 30 days THEN Income estimated from these tradelines should not get added to the income.
     *
     * @return return true if need to consider income into total_estimated_income else false
     */
    private Boolean loanToBeConsideredForIncome(JsonNode loan, String loanCode) {
        JsonNode openDate1 = loan.get(ExperianConstants.OPEN_DATE);
        Date openDate = null;
        try {
            openDate = dateFormat.parse(openDate1.asText());
        } catch (Exception e) {
            log.info("Exception while parsing date opened", e);
        }
        //? what condition to be added if openDate is null?
        if (ObjectUtils.isEmpty(openDate)) {
            return false;
        }
        Date today = new Date();
        long diffInDays = (Math.abs(today.getTime() - openDate.getTime())) / (1000 * 60 * 60 * 24);
        if ("CC".equals(loanCode)) {
            return diffInDays > 90;
        }
        return diffInDays > 30;
    }

    public Double getNetFreeIncome(Double estimatedIncome, Double debt) {
        Double expenses = 0.20 * estimatedIncome;
        double totalExpenses = expenses + debt;
        return estimatedIncome - totalExpenses;
    }

    private boolean isLoanClosed(JsonNode loan) {
        JsonNode closedDate = loan.get(ExperianConstants.DATE_CLOSED);
        Date clsDate = null;
        try {
            clsDate = dateFormat.parse(closedDate.asText());
        } catch (Exception e) {
            log.info("Exception while parsing date closed, {}", e.getMessage());
        }
        boolean isClosed = false;
        if (clsDate != null) {
            isClosed = !clsDate.after(new Date());
        }
        Integer accountStatus = loan.has(ExperianConstants.ACCT_STATUS)
                && !loan.get(ExperianConstants.ACCT_STATUS).isNull() ? (Integer) loan.get(ExperianConstants.ACCT_STATUS).asInt()
                : null;
        return isClosed || !activeStatusList.contains(accountStatus);
    }

    private Double getLoanAmount(JsonNode loan) {
        double amount1 = loan.has("Highest_Credit_or_Original_Loan_Amount")
                ? loan.get("Highest_Credit_or_Original_Loan_Amount").asDouble()
                : 0D;
        double amount2 = loan.has("Credit_Limit_Amount") ? loan.get("Credit_Limit_Amount").asDouble() : 0D;
        return Math.max(amount1, amount2);
    }

    private Double getCurrentBalance(JsonNode loan) {
        return loan.has(ExperianConstants.CURRENT_BALANCE) ? loan.get(ExperianConstants.CURRENT_BALANCE).asDouble() : 0D;
    }

    private String getLoanType(Integer loanType) {
        if (loanType == 1 || loanType == 17 || loanType == 32) {
            return "AL";
        }
        if (loanType == 4 || loanType == 5 || loanType == 9 || loanType == 38 || loanType == 39
                || loanType == 60) {
            return "PL";

        }
        if (loanType == 2 || loanType == 3) {
            return "HL";
        }
        if (loanType == 51 || loanType == 52 || loanType == 53 || loanType == 54 || loanType == 59
                || loanType == 61) {
            return "BL";
        }
        if (loanType == 10 || loanType == 35) {
            return "CC";
        }
        if (loanType == 13) {
            return "TW";
        }
        if (loanType == 6) {
            return "CD";
        }
        if (loanType == 7) {
            return "GL";
        }
        return "Other";
    }
}
