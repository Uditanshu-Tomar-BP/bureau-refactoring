package com.bharatpe.lending.bureaurefactoring.nfi;

import com.bharatpe.lending.bureaurefactoring.constants.Constant;
import com.bharatpe.lending.bureaurefactoring.constants.CrifConstants;
import com.bharatpe.lending.bureaurefactoring.dto.DebtAndIncomeDto;
import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.*;
@Slf4j
public class CrifNfiCalculator implements NfiCalculator {
    SimpleDateFormat dateFormat = new SimpleDateFormat(CrifConstants.DATE_FORMAT);
    List<String> loanTypeAL = Arrays.asList("auto loan (personal)", "commercial vehicle loan", "used car loan");
    List<String> loanTypeBL = Arrays.asList("business loan - secured", "business loan against bank deposits",
            "business loan general", "business loan priority sector agriculture",
            "business loan priority sector others", "business loan priority sector small business",
            "business loan unsecured", "microfinance business loan", "od on savings account");
    List<String> loanTypeCC = Arrays.asList("corporate credit card", "credit card", "secured credit card");
    List<String> loanTypeCD = Collections.singletonList("consumer loan");
    List<String> loanTypeGL = Collections.singletonList("gold loan");
    List<String> loanTypeHL = Arrays.asList("housing loan", "microfinance housing loan",
            "pradhan mantri awas yojana - clss", "property loan");
    List<String> loanTypePL = Arrays.asList("loan against shares / securities", "loan to professional",
            "microfinance personal loan", "mudra loans – shishu / kishor / tarun", "personal loan",
            "prime minister jaan dhan yojana - overdraft", "staff loan");
    List<String> loanTypeTW = Collections.singletonList("two-wheeler loan");

    @Override
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
            DebtAndIncomeDto debtAndIncomeDto = getDebtAndIncomeOfALoan(loan.get(CrifConstants.LOAN_DETAILS));
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
        if (loan == null || loan.get(CrifConstants.ACCT_TYPE) == null
                || loan.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("\"\"")) {
            return debtAndIncome;
        }
        String loanCode = getLoanType(loan.get(CrifConstants.ACCT_TYPE).asText());
        double loanAmount = getLoanAmount(loan);
        boolean isLoanClosed = isLoanClosed(loan);

        debtAndIncome.setLoanCode(loanCode);
        debtAndIncome.setLoanAmount(loanAmount);
        debtAndIncome.setCloseDate(String.valueOf(loan.get(CrifConstants.CLOSED_DATE)));

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
        debtAndIncome.setOpenDate(String.valueOf(loan.get(CrifConstants.DISBURSED_DT)));

        if (!loanToBeConsideredForIncome(loan, loanCode))
            return debtAndIncome;
        double dbi = Constant.DBI_PROPORTION.get(loanCode);
        double dbi_pp = Constant.DBI_PROPORTION_PP.get(loanCode);
        debtAndIncome.setDbiProportion(dbi);
        debtAndIncome.setDbiProportionPostPe(dbi_pp);
        if (dbi != 0) {
            Double income = debt / dbi;
            debtAndIncome.setIncome(income);
        }
        if (dbi_pp != 0) {
            double incomePostPe = "CC".equals(loanCode) ? loanAmount * Constant.CREDIT_CARD_INCOME_POSTPE_MULTIPLIER : debt / dbi_pp;
            debtAndIncome.setIncomePostPe(incomePostPe);
        }
        return debtAndIncome;
    }

    private Boolean loanToBeConsideredForIncome(JsonNode loan, String loanCode) {
        JsonNode openDate1 = loan.get(CrifConstants.DISBURSED_DT);
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

    private boolean isLoanClosed(JsonNode loan) {
        JsonNode closedDate = loan.has(CrifConstants.CLOSED_DATE) && loan.get(CrifConstants.CLOSED_DATE) != null ? loan.get(CrifConstants.CLOSED_DATE) : null;
        Date clsDate = null;
        try {
            clsDate = closedDate != null ? dateFormat.parse(closedDate.asText()) : null;
        } catch (Exception e) {
            log.info("Exception while parsing date closed");
        }
        if (clsDate != null && !clsDate.after(new Date())) {
            return true;
        }
        return loan.get(CrifConstants.ACCT_STATUS) != null
                && (loan.get(CrifConstants.ACCT_STATUS).toString().equalsIgnoreCase("\"Closed\""));
    }

    private Double getLoanAmount(JsonNode loan) {
        double amount = 0D;
        if (loan.get(CrifConstants.DISBURSED_AMT) != null) {
            amount = Double.parseDouble(loan.get(CrifConstants.DISBURSED_AMT).asText().replace(",", ""));
        }
        if (loan.get(CrifConstants.CREDIT_LIMIT) != null) {
            amount = Math.max(Double.parseDouble(loan.get(CrifConstants.CREDIT_LIMIT).asText().replace(",", "")), amount);
        }
        return amount;
    }

    private Double getCurrentBalance(JsonNode loan) {
        double amount = 0D;
        if (loan.get("CURRENT-BAL") != null) {
            amount = Double.parseDouble(loan.get("CURRENT-BAL").asText().replace(",", ""));
        }
        return amount;
    }


    private String getLoanType(String loanType) {
        if (loanType == null || loanType.isEmpty()) return "Other";
        loanType = loanType.toLowerCase();
        if (loanTypeAL.contains(loanType)) return "AL";
        if (loanTypePL.contains(loanType)) return "PL";
        if (loanTypeHL.contains(loanType)) return "HL";
        if (loanTypeBL.contains(loanType)) return "BL";
        if (loanTypeCC.contains(loanType)) return "CC";
        if (loanTypeTW.contains(loanType)) return "TW";
        if (loanTypeCD.contains(loanType)) return "CD";
        if (loanTypeGL.contains(loanType)) return "GL";
        return "Other";
    }
}
