package com.bharatpe.lending.bureaurefactoring.utils;

import com.bharatpe.lending.bureaurefactoring.dto.CreditScoreReportDetailDTO;
import com.bharatpe.lending.bureaurefactoring.dto.LoanAndCreditCardDetailDTO;
import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;
import com.bharatpe.lending.bureaurefactoring.enums.Gender;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

public interface BureauResponseUtil {
    int fetchBureauVintage();

    Date getReportDate();

    String getType();

    boolean isValid(String panCard, String phoneNumber);

    String getEmail();

    int countLoanEnquiriesInLast3Months();

    int countUnsecuredLoanEnquiriesInLast6Months() throws ParseException;

    Double getBureauScore();

    JsonNode getResponse();

    Map<String, Object> getBBSCalculationDetails() throws IOException, ParseException;

    int getLoanCount(BureauLoanType loanType);

    Integer getAge();

    String getDOB();

    int getMaxDPD(int months);

    boolean writtenOffLast12Months();

    int getMaxOverdueAmount();

    int countUnsecuredLoanEnquiries(int months);

    int countSecuredLoanEnquiries(int months);

    double maxLoanAmount(BureauLoanType loanType);

    double maxCurrentBalance(BureauLoanType loanType);

    double minLoanAmount(BureauLoanType loanType);

    double totalLoanAmount(BureauLoanType loanType);

    Integer getVintage(BureauLoanType loanType);

    int getTotalLoanCount();

    String getPancard();

    List<String> getAddress();

    CreditScoreReportDetailDTO getCreditDetailReport(JsonNode bureauResponse);

    LoanAndCreditCardDetailDTO getLoanAndCreditDetail(JsonNode bureauResponse);

    Gender getGender();

    double unsecuredLoanUtilization();

    Double nonCreditOverDuePastXMonthAndActive(int month);

    Double creditCardOverDuePastXMonth(int month);

    Double closedLoanWithOverDueLastXMonth(int month);

    Double settleLoanPastXMonth(int month);

    int maxCreditCardTradeMoreThan60(int maxDpd);

    int maxNonCreditCardTradeMoreThan60(int maxDpd);

    int totalEnquiryLastXMonth(int month);

    int activeGoldLoan(Double amount);

    int activePersonalLoan(Double amount);

    int tradeDpdLessThan(int minDpd,int maxDpd,int month);

    int tradeDpdMoreThan(int minDpd,int maxDpd,int month);

    int subDPDMoreThan(int maxDpd,int month);

    int subDPDLessThan(int maxDpd,int month);

    Boolean getETC();

    Boolean getLoanSettlement();
}
