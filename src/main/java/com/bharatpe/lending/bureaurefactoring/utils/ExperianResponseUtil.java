package com.bharatpe.lending.bureaurefactoring.utils;

import com.bharatpe.lending.bureaurefactoring.constants.Constant;
import com.bharatpe.lending.bureaurefactoring.constants.ExperianConstants;
import com.bharatpe.lending.bureaurefactoring.dto.CreditScoreReportDetailDTO;
import com.bharatpe.lending.bureaurefactoring.dto.LoanAndCreditCardDetailDTO;
import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;
import com.bharatpe.lending.bureaurefactoring.enums.Gender;
import com.bharatpe.lending.bureaurefactoring.nfi.ExperianNfiCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.lang.Math.max;

public class ExperianResponseUtil extends ResponseUtilBase implements BureauResponseUtil {
    ExperianNfiCalculator experianNfiCalculator = new ExperianNfiCalculator();

    Logger logger = LoggerFactory.getLogger(ExperianResponseUtil.class);

    List<Integer> derogUnsecuredProducts = Arrays.asList(5, 10, 36, 37, 38, 39, 43, 51, 52, 53, 54, 55, 56, 57, 58, 60, 61);
    List<Integer> derogAccountStatus = Arrays.asList(93, 89, 93, 97, 97, 97, 97, 30, 31, 32, 33, 35, 37, 38, 39, 41, 42,
            43, 44, 45, 47, 49, 50, 51, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 72, 73,
            74, 75, 76, 77, 79, 81, 85, 86, 87, 88, 94, 90, 91);

    List<Integer> activeStatusList = Arrays.asList(11, 21, 22, 23, 24, 25, 71, 78, 80, 82, 83, 84);

    List<Integer> closedStatusList = Arrays.asList(13, 14, 15, 16, 17);

    List<Integer> unsecuredLoan = Arrays.asList(0, 5, 6, 8, 9, 10, 11, 12, 14, 16, 18, 19, 20, 31, 35, 36, 37, 38, 39,
            43, 51, 52, 53, 54, 55, 56, 57, 58, 61);

    List<Integer> unsecuredLoanUnderGstOffer = Arrays.asList(5, 6, 8, 9, 10, 12, 14, 16, 35, 36, 37, 38, 39, 43, 51, 52, 53, 54, 55, 56, 57, 58, 61);


    SimpleDateFormat dateFormat = new SimpleDateFormat(ExperianConstants.DATE_FORMAT);

    DateTimeFormatter formatter = DateTimeFormat.forPattern(ExperianConstants.DATE_FORMAT);

    List<String> assetList = Arrays.asList("SUB", "DBT", "LSS", "WOF", "SMA", "B", "D", "M", "L");

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    List<Integer> writtenOffAccStatus = Arrays.asList(00, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 72, 73, 74, 75, 76, 77, 79, 81, 85, 86, 87, 88, 90, 91, 93, 97);


    public ExperianResponseUtil(JsonNode response) {
        this.type = Bureau.EXPERIAN.name();
        this.response = response;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public JsonNode getResponse() {
        return this.response;
    }

    @Override
    public boolean isValid(String panCard, String phoneNumber) {
        return this.response != null;
    }

    @Override
    public Date getReportDate() {
        try {
            return dateFormat
                    .parse(response.get(ExperianConstants.PROFILE_RESPONSE).get("CreditProfileHeader").get("ReportDate").asText());
        } catch (ParseException e) {
            logger.info("Exception in parsing report date", e);
            return null;
        }
    }

    @Override
    public String getEmail() {
        String email = null;
        try {
            if (Objects.isNull(response)) {
                return email;
            }
            logger.info("response in get email: {}", response.get(ExperianConstants.PROFILE_RESPONSE));
            JsonNode currentApplicationDetails = null;
            if (Objects.nonNull(response.get(ExperianConstants.PROFILE_RESPONSE))) {
                currentApplicationDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get("Current_Application")
                        .get("Current_Application_Details");
            }
            if (currentApplicationDetails != null && currentApplicationDetails.get("Current_Applicant_Details") != null) {
                email = currentApplicationDetails.get("Current_Applicant_Details").get("EMailId").asText();
            }
        } catch (Exception e) {
            logger.error("Exception while getting email: {} {}", e.getMessage(), e);
        }
        return email;

    }

    @Override
    public Double getBureauScore() {
        JsonNode bureauScore = null;

        try {
            bureauScore = response.get(ExperianConstants.PROFILE_RESPONSE).get("SCORE").get("BureauScore");
        } catch (NullPointerException e) {
            logger.error("Unable to get bureau score.");
        }

        return bureauScore != null ? bureauScore.doubleValue() : 0D;
    }

    @Override
    public int fetchBureauVintage() {
        DateTime min = new DateTime();
        if (Objects.isNull(response)) {
            return 0;
        }
        if (Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE)) ||
                Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT))
        ) {
            return 0;
        }
        JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)
                .get(ExperianConstants.ACCT_DETAILS);
        if (accountDetails != null && accountDetails.isArray()) {
            for (JsonNode jsonNode : accountDetails) {
                try {
                    min = formatter.parseDateTime(jsonNode.get(ExperianConstants.OPEN_DATE).toString()).isBefore(min)
                            ? formatter.parseDateTime(jsonNode.get(ExperianConstants.OPEN_DATE).toString())
                            : min;
                } catch (Exception e) {
                    logger.info("Invalid Open_Date");
                }
            }
            return Days.daysBetween(min, DateTime.now()).getDays();
        }

        if (accountDetails != null && accountDetails.isObject()) {
            try {
                min = formatter.parseDateTime(accountDetails.get(ExperianConstants.OPEN_DATE).toString()).isBefore(min)
                        ? formatter.parseDateTime(accountDetails.get(ExperianConstants.OPEN_DATE).toString())
                        : min;
            } catch (Exception e) {
                logger.info("Invalid Open_Date");
            }
            return Days.daysBetween(min, DateTime.now()).getDays();
        }
        return 0;
    }

    public String fetchAccountCategory() {
        List<Integer> categoryA = Arrays.asList(6, 7, 13, 38, 39, 43);
        List<Integer> categoryB = Arrays.asList(1, 5, 8, 9, 10, 11, 12, 17, 32, 33, 34, 36, 37, 51, 52, 53, 54, 55, 56,
                57, 58, 59, 60, 61);
        List<Integer> categoryC = Arrays.asList(2, 3);
        boolean a = false, b = false, c = false;
        JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)
                .get(ExperianConstants.ACCT_DETAILS);
        if (accountDetails != null && accountDetails.isArray()) {
            for (JsonNode jsonNode : accountDetails) {
                if (categoryA.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    a = true;
                }
                if (categoryB.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    b = true;
                }
                if (categoryC.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    c = true;
                }
            }
        } else if (accountDetails != null && accountDetails.isObject()) {
            if (categoryA.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                a = true;
            }
            if (categoryB.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                b = true;
            }
            if (categoryC.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                c = true;
            }
        }
        return c ? "C" : b ? "B" : a ? "A" : "NTC";
    }

    private boolean derogChecks(JsonNode jsonNode, Date reportDate) {
        if (jsonNode.get(ExperianConstants.ACCT_STATUS) != null
                && derogAccountStatus.contains(jsonNode.get(ExperianConstants.ACCT_STATUS).asInt())) {
            logger.info("Derog Account Status check failed, rejecting merchant:");
            rejectReason = ExperianConstants.DEROG_ACCOUNT_STATUS;
            return true;
        }
        if (jsonNode.get(ExperianConstants.ACCT_HOLDER_TYPE_CODE).asInt() != 7
                && checkDPDLastXmonths(jsonNode, 3, reportDate)) {
            rejectReason = ExperianConstants.DEROG_DPD_LAST_3_MONTHS;
            return true;
        }
        if (jsonNode.get(ExperianConstants.ACCT_HOLDER_TYPE_CODE).asInt() != 7
                && checkDPDLastXmonths(jsonNode, 6, reportDate)) {
            logger.info("Derog DPD Last 6 months check failed, rejecting merchant: {}", "merchantId");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_6_MONTHS;
            return true;
        }
        if (jsonNode.get(ExperianConstants.ACCT_HOLDER_TYPE_CODE).asInt() != 7
                && checkDPDLastXmonths(jsonNode, 12, reportDate)) {
            logger.info("Derog DPD Last 12 months check failed, rejecting merchant: {}", "merchantId");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_12_MONTHS;
            return true;
        }
        if (jsonNode.get(ExperianConstants.ACCT_HOLDER_TYPE_CODE).asInt() != 7
                && checkDPDLastXmonths(jsonNode, 24, reportDate)) {
            logger.info("Derog DPD Last 24 months check failed, rejecting merchant: {}", "merchantId");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_24_MONTHS;
            return true;
        }
        return false;
    }

    private boolean checkDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        Date dateReported = null;
        try {
            if (jsonNode.get(ExperianConstants.DATE_REPORTED) != null
                    && !jsonNode.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                dateReported = dateFormat.parse(jsonNode.get(ExperianConstants.DATE_REPORTED).asText());
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<String> monthYear = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > months * 30) {
            return false;
        }
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) <= months * 30) {
            c.setTime(dateReported);
        } else {
            c.setTime(reportDate);
        }
        String month;
        int dpd = 5;// 3 months
        switch (months) {
            case 6:
                dpd = 30;
                break;
            case 12:
                dpd = 60;
                break;
            case 24:
                dpd = 90;
                break;
            default:
                break;
        }
        for (int i = 0; i < months; i++) {
            month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1)
                    : (c.get(Calendar.MONTH) + 1) + "";
            monthYear.add(month + "$" + c.get(Calendar.YEAR));// 01$2020
            c.add(Calendar.MONTH, -1);
        }
        if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isArray()) {
            for (JsonNode history : jsonNode.get(ExperianConstants.ACCT_HISTORY)) {
                if (monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                        && !history.get(ExperianConstants.DPD).isNull()
                        && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                        && history.get(ExperianConstants.DPD).asInt() >= dpd) {
                    return true;
                }
            }
        } else if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isObject()) {
            JsonNode history = jsonNode.get(ExperianConstants.ACCT_HISTORY);
            return monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                    && !history.get(ExperianConstants.DPD).isNull()
                    && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                    && history.get(ExperianConstants.DPD).asInt() >= dpd;
        }
        return false;
    }

    private int countDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        Date dateReported = null;
        try {
            if (jsonNode.get(ExperianConstants.DATE_REPORTED) != null
                    && !jsonNode.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                dateReported = dateFormat.parse(jsonNode.get(ExperianConstants.DATE_REPORTED).asText());
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<String> monthYear = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > months * 30) {
            return 0;
        }
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) <= months * 30) {
            c.setTime(dateReported);
        } else {
            c.setTime(reportDate);
        }
        String month;
        int dpd = 0;
        for (int i = 0; i < months; i++) {
            month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1)
                    : (c.get(Calendar.MONTH) + 1) + "";
            monthYear.add(month + "$" + c.get(Calendar.YEAR));// 01$2020
            c.add(Calendar.MONTH, -1);
        }
        if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isArray()) {
            for (JsonNode history : jsonNode.get(ExperianConstants.ACCT_HISTORY)) {
                if (monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                        && !history.get(ExperianConstants.DPD).isNull()
                        && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                        && history.get(ExperianConstants.DPD).asInt() > 0) {
                    dpd++;
                }
            }
        } else if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isObject()) {
            JsonNode history = jsonNode.get(ExperianConstants.ACCT_HISTORY);
            if (monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                    && !history.get(ExperianConstants.DPD).isNull()
                    && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                    && history.get(ExperianConstants.DPD).asInt() > 0) {
                dpd++;
            }
        }
        return dpd;
    }

    private int loanSanctioned3mon(JsonNode jsonNode, Date reportDate) throws ParseException {
        if (jsonNode.get(ExperianConstants.OPEN_DATE) != null
                && !jsonNode.get(ExperianConstants.OPEN_DATE).toString().equalsIgnoreCase("\"\"")) {
            Date openDate = dateFormat.parse(jsonNode.get(ExperianConstants.OPEN_DATE).asText());
            return CommonUtil.getDateDiffInDays(openDate, reportDate) <= 90 ? 1 : 0;
        }
        return 0;
    }

    private int unsecuredLoan6mon(JsonNode jsonNode, Date reportDate) throws ParseException {
        if (jsonNode.get(ExperianConstants.ACCT_TYPE) != null && jsonNode.get(ExperianConstants.OPEN_DATE) != null
                && !jsonNode.get(ExperianConstants.OPEN_DATE).toString().equalsIgnoreCase("\"\"")) {
            Date openDate = dateFormat.parse(jsonNode.get(ExperianConstants.OPEN_DATE).asText());
            return CommonUtil.getDateDiffInDays(openDate, reportDate) <= 180
                    && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt()) ? 1 : 0;
        } else if (jsonNode.get(ExperianConstants.ACCT_TYPE) != null
                && jsonNode.get(ExperianConstants.DATE_ADDITION) != null
                && !jsonNode.get(ExperianConstants.DATE_ADDITION).toString().equalsIgnoreCase("\"\"")) {
            Date openDate = dateFormat.parse(jsonNode.get(ExperianConstants.DATE_ADDITION).asText());
            return CommonUtil.getDateDiffInDays(openDate, reportDate) <= 180
                    && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt()) ? 1 : 0;
        }
        return 0;
    }

    private boolean isLoanClosed(JsonNode loan) {
        JsonNode closedDate = loan.has(ExperianConstants.DATE_CLOSED) && loan.get(ExperianConstants.DATE_CLOSED) != null ? loan.get(ExperianConstants.DATE_CLOSED) : null;
        Date clsDate = null;
        try {
            clsDate = closedDate != null ? dateFormat.parse(closedDate.asText()) : null;
        } catch (Exception e) {
            logger.info("Exception while parsing date closed");
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
        return amount2 > 0d ? amount2 : max(amount1, amount2);
    }

    private Double getCurrentBalance(JsonNode loan) {
        double amount = loan.has(ExperianConstants.CURRENT_BALANCE)
                ? loan.get(ExperianConstants.CURRENT_BALANCE).asDouble()
                : 0D;

        return amount;
    }

    private boolean isLoanClosedWithinOneYear(JsonNode loan) {
        String date = (loan.has(ExperianConstants.DATE_CLOSED) && !loan.get(ExperianConstants.DATE_CLOSED).isNull()
                && !loan.get(ExperianConstants.DATE_CLOSED).asText().equalsIgnoreCase(""))
                ? loan.get(ExperianConstants.DATE_CLOSED).asText()
                : null;
        if (date != null) {
            try {
                Date closingDate = dateFormat.parse(date);
                Date today = new Date();
                return ((today.getTime() - closingDate.getTime()) / 1000) <= 31556952;
            } catch (Exception e) {
                logger.error("Error occured while checking for loan closing duration", e);
            }
        }
        return false;
    }

    private String getLoanType(Integer loanType) {
        if (loanType == 1 || loanType == 17 || loanType == 32) {
            return "AL";
        } else if (loanType == 4 || loanType == 5 || loanType == 9 || loanType == 38 || loanType == 39
                || loanType == 60) {
            return "PL";

        } else if (loanType == 2 || loanType == 3) {
            return "HL";
        } else if (loanType == 51 || loanType == 52 || loanType == 53 || loanType == 54 || loanType == 59
                || loanType == 61) {
            return "BL";
        } else if (loanType == 10 || loanType == 35) {
            return "CC";
        } else if (loanType == 13) {
            return "TW";
        } else if (loanType == 6) {
            return "CD";
        } else if (loanType == 7) {
            return "GL";
        } else {
            return "Other";
        }
    }

    private String getD2rLoanType(Integer loanType) {
        if (loanType == 1 || loanType == 17 || loanType == 32) {
            return "AL";
        } else if (loanType == 4 || loanType == 5 || loanType == 38 || loanType == 60) {
            return "PL";
        } else if (loanType == 2 || loanType == 3) {
            return "HL";
        } else if (loanType == 35 || loanType == 39 || loanType == 51 || loanType == 52 || loanType == 53
                || loanType == 54 || loanType == 55 || loanType == 56 || loanType == 57
                || loanType == 58 || loanType == 59 || loanType == 61 || loanType == 9 || loanType == 12) {
            return "BL";
        } else if (loanType == 10) {
            return "CC";
        } else if (loanType == 13) {
            return "TW";
        } else if (loanType == 6) {
            return "CD";
        } else if (loanType == 7) {
            return "GL";
        } else {
            return "Other";
        }
    }

    private Map<String, Double> getDebtAndIncome(JsonNode loan) {
        Map<String, Double> debtAndIncome = new HashMap<>();
        double debt = 0D;
        double income = 0D;
        double debtActiveLoans = 0D;
        if (loan.get(ExperianConstants.ACCT_TYPE) == null) {
            debtAndIncome.put("debt", debt);
            debtAndIncome.put("income", income);
            debtAndIncome.put("debtActiveLoans", debtActiveLoans);
            return debtAndIncome;
        }
        int loanTypeNumber = loan.get(ExperianConstants.ACCT_TYPE).asInt();
        double loanAmount = getLoanAmount(loan);
        boolean isLoanClosed = isLoanClosed(loan);
        boolean isLoanClosedWithinAYear = isLoanClosedWithinOneYear(loan);
        String loanType = getLoanType(loanTypeNumber);
        String d2rLoanType = getD2rLoanType(loanTypeNumber);
        if (loanAmount >= 5000 && (!isLoanClosed || isLoanClosedWithinAYear)) {
            if (!isLoanClosedWithinAYear) {
                debt += loanAmount * Constant.EMI.get(loanType);
            }
            income += loanAmount * Constant.EMI.get(loanType) / Constant.DBI.get(loanType);
            if (income < Constant.OTHER_INCOME.getOrDefault(loanType, 0D)) {
                income = Constant.OTHER_INCOME.getOrDefault(loanType, 0D);
            }
            if (!isLoanClosed) {
                debtActiveLoans += loanAmount * ExperianConstants.DEBT_EMI.getOrDefault(d2rLoanType, 0D) / 100000;
            }
            logger.info(
                    "loanStatus: {}, loanAmount:{}, isLoanClosed: {}, isLoanClosedWithinAYear: {}, loanType: {}, d2rLoanTYpe: {}, income:{}, debt:{}, debtActiveLoans: {}",
                    loan.get(ExperianConstants.ACCT_STATUS), loanAmount, isLoanClosed, isLoanClosedWithinAYear,
                    loanType, d2rLoanType, income, debt, debtActiveLoans);
        }
        debtAndIncome.put("debt", debt);
        debtAndIncome.put("income", income);
        debtAndIncome.put("debtActiveLoans", debtActiveLoans);
        return debtAndIncome;
    }

    private Map<String, Double> getDebtAndIncome(ArrayNode loanDetails) {
        Map<String, Double> debtAndIncome = new HashMap<>();
        Map<String, Double> incomeMap = new HashMap<>();
        double debt = 0D;
        double debtActiveLoans = 0D;
        for (JsonNode loan : loanDetails) {
            if (loan.get(ExperianConstants.ACCT_TYPE) == null) {
                continue;
            }
            int loanTypeNumber = loan.get(ExperianConstants.ACCT_TYPE).asInt();
            double loanAmount = getLoanAmount(loan);
            boolean isLoanClosed = isLoanClosed(loan);
            boolean isLoanClosedWithinAYear = isLoanClosedWithinOneYear(loan);
            String loanType = getLoanType(loanTypeNumber);
            String d2rLoanType = getD2rLoanType(loanTypeNumber);
            if (loanAmount >= 5000 && (!isLoanClosed || isLoanClosedWithinAYear)) {
                double income = loanAmount * Constant.EMI.get(loanType) / Constant.DBI.get(loanType);
                incomeMap.put(loanType, incomeMap.getOrDefault(loanType, 0D) + income);
                if (!isLoanClosedWithinAYear) {
                    debt += loanAmount * Constant.EMI.get(loanType);
                }
                if (!isLoanClosed) {
                    debtActiveLoans += loanAmount * ExperianConstants.DEBT_EMI.getOrDefault(d2rLoanType, 0D) / 100000;
                }
                logger.info(
                        "loanStatus: {}, loanAmount:{}, isLoanClosed: {}, isLoanClosedWithinAYear: {}, loanType: {}, d2rLoanTYpe: {} income:{}, debt:{}, debtActiveLoans; {}",
                        loan.get(ExperianConstants.ACCT_STATUS), loanAmount, isLoanClosed, isLoanClosedWithinAYear,
                        loanType, d2rLoanType, income, debt, debtActiveLoans);
            }
        }
        double totalIncome = 0d;
        for (String loanType : incomeMap.keySet()) {
            totalIncome += max(incomeMap.get(loanType), Constant.OTHER_INCOME.getOrDefault(loanType, 0D));
        }
        debtAndIncome.put("debt", debt);
        debtAndIncome.put("income", totalIncome);
        debtAndIncome.put("debtActiveLoans", debtActiveLoans);
        return debtAndIncome;
    }

    private boolean checkUnsecuredLiveLoans(JsonNode jsonNode) {
        return jsonNode.get(ExperianConstants.DATE_CLOSED).toString().equals("\"\"")
                && jsonNode.get(ExperianConstants.ACCT_TYPE).asInt() != 10
                && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt());
    }

    @Override
    public int countLoanEnquiriesInLast3Months() {
        try {
            if (response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY) != null
                    && response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY)
                    .get("TotalCAPSLast90Days") != null)
                return response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY)
                        .get("TotalCAPSLast90Days").asInt();
        } catch (Exception e) {
            logger.error("Exception while checking loan enquiries");
        }
        return 0;
    }

    @Override
    public int countUnsecuredLoanEnquiriesInLast6Months() {
        Date reportDate = getReportDate();
        Calendar c = Calendar.getInstance();
        c.setTime(reportDate);
        c.add(Calendar.MONTH, -6);
        String month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1)
                : (c.get(Calendar.MONTH) + 1) + "";
        String day = (c.get(Calendar.DAY_OF_MONTH) + 1) < 10 ? "0" + (c.get(Calendar.DAY_OF_MONTH) + 1)
                : (c.get(Calendar.DAY_OF_MONTH) + 1) + "";
        long previous6MonthDate = Long.parseLong(c.get(Calendar.YEAR) + month + day);
        if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null
                && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS)
                .isObject()) {
            JsonNode jsonNode = response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS")
                    .get(ExperianConstants.CAPS_DETAILS);
            return jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null && jsonNode.get(ExperianConstants.DOR) != null
                    && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                    && jsonNode.get(ExperianConstants.DOR).longValue() >= previous6MonthDate ? 1 : 0;
        } else if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS")
                .get(ExperianConstants.CAPS_DETAILS) != null
                && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS)
                .isArray()) {
            for (JsonNode jsonNode : response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS")
                    .get(ExperianConstants.CAPS_DETAILS)) {
                if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null
                        && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                        && jsonNode.get(ExperianConstants.DOR) != null
                        && jsonNode.get(ExperianConstants.DOR).longValue() >= previous6MonthDate) {
                    return 1;
                }
            }
        }
        return 0;
    }

    @Override
    public Map<String, Object> getBBSCalculationDetails() throws ParseException {
        Map<String, Object> res = new HashMap<>();
        NfiCalculationDetailsDto nfiCalculationDetailsDto = null;
        Date reportDate = getReportDate();
        int delinquencyCount6mon = 0;
        int loanSanctioned3mon = 0;
        int unsecuredLoanCount6mon = 0;
        Double activeUslPos = null;
        Date minOpenDate = reportDate;
        Set<Integer> loanTypes = new HashSet<>();
        JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)
                .get(ExperianConstants.ACCT_DETAILS);
        if (accountDetails != null && accountDetails.isObject()) {
            nfiCalculationDetailsDto = experianNfiCalculator.getDebtAndIncomeDetails(accountDetails);
            activeUslPos = getActiveUslPos(accountDetails);
            delinquencyCount6mon += countDPDLastXmonths(accountDetails, 6, reportDate);
            loanSanctioned3mon += loanSanctioned3mon(accountDetails, reportDate);
            unsecuredLoanCount6mon += unsecuredLoan6mon(accountDetails, reportDate);
            if (accountDetails.get(ExperianConstants.ACCT_TYPE) != null) {
                loanTypes.add(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt());
            }
            if (accountDetails.get(ExperianConstants.OPEN_DATE) != null
                    && !accountDetails.get(ExperianConstants.OPEN_DATE).toString().equalsIgnoreCase("\"\"")) {
                Date openDate = dateFormat.parse(accountDetails.get(ExperianConstants.OPEN_DATE).asText());
                if (openDate.before(minOpenDate)) {
                    minOpenDate = openDate;
                }
            } else if (accountDetails.get(ExperianConstants.DATE_ADDITION) != null
                    && !accountDetails.get(ExperianConstants.DATE_ADDITION).toString().equalsIgnoreCase("\"\"")) {
                Date openDate = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_ADDITION).asText());
                if (openDate.before(minOpenDate)) {
                    minOpenDate = openDate;
                }
            }
        } else if (accountDetails != null && accountDetails.isArray()) {
            nfiCalculationDetailsDto = experianNfiCalculator.getDebtAndIncomeDetails((ArrayNode) accountDetails);
            activeUslPos = getActiveUslPos(accountDetails);
            for (JsonNode caisAccountDetails : accountDetails) {
                delinquencyCount6mon += countDPDLastXmonths(caisAccountDetails, 6, reportDate);
                loanSanctioned3mon += loanSanctioned3mon(caisAccountDetails, reportDate);
                unsecuredLoanCount6mon += unsecuredLoan6mon(caisAccountDetails, reportDate);
                if (caisAccountDetails.get(ExperianConstants.ACCT_TYPE) != null) {
                    loanTypes.add(caisAccountDetails.get(ExperianConstants.ACCT_TYPE).asInt());
                }
                if (caisAccountDetails.get(ExperianConstants.OPEN_DATE) != null
                        && !caisAccountDetails.get(ExperianConstants.OPEN_DATE).toString().equalsIgnoreCase("\"\"")) {
                    Date openDate = dateFormat.parse(caisAccountDetails.get(ExperianConstants.OPEN_DATE).asText());
                    if (openDate.before(minOpenDate)) {
                        minOpenDate = openDate;
                    }
                } else if (caisAccountDetails.get(ExperianConstants.DATE_ADDITION) != null
                        && !caisAccountDetails.get(ExperianConstants.DATE_ADDITION).toString().equalsIgnoreCase("\"\"")) {
                    Date openDate = dateFormat.parse(caisAccountDetails.get(ExperianConstants.DATE_ADDITION).asText());
                    if (openDate.before(minOpenDate)) {
                        minOpenDate = openDate;
                    }
                }
            }
        }
        res.put("activeUnsecuredLoanAmount", activeUslPos);
        res.put("debtAndIncome", nfiCalculationDetailsDto);
        res.put("delinquencyCount6mon", delinquencyCount6mon);
        res.put("loanSanctioned3mon", loanSanctioned3mon);
        res.put("unsecuredLoanCount6mon", unsecuredLoanCount6mon);
        res.put("minOpenDate", minOpenDate);
        res.put("loanTypes", loanTypes);
        return res;
    }

    /**
     * Getting active unsecured loan principal outstanding amount.
     *
     * @param loanDetails
     * @return
     */
    private Double getActiveUslPos(JsonNode loanDetails) {
        Double activeUslPos = 0d;
        try {
            if (!loanDetails.isArray()) {
                logger.info("getActiveUslPos -> loan: {}", loanDetails);
                int loanTypeNumber = loanDetails.get(ExperianConstants.ACCT_TYPE).asInt();
                boolean isLoanClosed = isLoanClosed(loanDetails);
                if (!isLoanClosed && unsecuredLoanUnderGstOffer.contains(loanTypeNumber)) {
                    return Math.abs(getCurrentBalance(loanDetails));
                }
                return activeUslPos;
            }
            for (JsonNode loan : loanDetails) {
                logger.info("getActiveUslPos -> loan: {}", loan);
                int loanTypeNumber = loan.get(ExperianConstants.ACCT_TYPE).asInt();
                boolean isLoanClosed = isLoanClosed(loan);
                if (!isLoanClosed && unsecuredLoanUnderGstOffer.contains(loanTypeNumber)) {
                    activeUslPos = ObjectUtils.isEmpty(activeUslPos) ? 0 : activeUslPos;
                    activeUslPos += Math.abs(getCurrentBalance(loan));
                }
            }
            return activeUslPos;
        } catch (Exception e) {
            logger.error("Exception occurred while fetching active Usl amount", e);
        }
        return null;
    }

    @Override
    public int getLoanCount(BureauLoanType loanType) {
        int count = 0;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return count;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            count++;
                        } else if (getLoanAmount(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {//credit card with amount>=10k and active
                            count++;
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        count++;
                    } else if (getLoanAmount(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {//credit card with amount>=10k and active
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getLoanCount in experian", e);
        }
        return count;
    }

    @Override
    public Integer getAge() {
        Integer age = null;
        try {
            String dob = getDOB();
            if (dob != null) {
                Date dateOfBirth = simpleDateFormat.parse(dob);
                age = CommonUtil.getDateDiffInYears(dateOfBirth, new Date());
            }
        } catch (Exception e) {
            logger.info("Exception while getting age from experian", e);
        }
        return age;
    }

    @Override
    public Boolean getLoanSettlement() {
        if (Objects.nonNull(response)) {
            return true;
        }
        try {
            if (response.get(ExperianConstants.ACCT_STATUS) != null
                    && derogAccountStatus.contains(response.get(ExperianConstants.ACCT_STATUS).asInt())) {
                logger.info("Derog Account Status check failed, rejecting merchant:");
                return false;
            }
        } catch (Exception e) {
            logger.error("Exception while checking loan settlement: {} {}", e.getMessage(), e);
        }
        return true;
    }

    @Override
    public String getDOB() {
        String dob = null;
        try {
            Date dateOfBirth = null;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (jsonNode.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH) != null && !jsonNode.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH).toString().equalsIgnoreCase("\"\"")) {
                        dateOfBirth = dateFormat.parse(jsonNode.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH).asText());
                        break;
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH) != null && !accountDetails.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH).toString().equalsIgnoreCase("\"\"")) {
                    dateOfBirth = dateFormat.parse(accountDetails.get(ExperianConstants.ACC_HOLDER_DETAILS).get(ExperianConstants.DATE_OF_BIRTH).asText());
                }
            }
            if (dateOfBirth != null) {
                dob = simpleDateFormat.format(dateOfBirth);
            }
        } catch (Exception e) {
            logger.info("Exception while getting DOB from experian", e);
        }
        return dob;
    }

    @Override
    public int getMaxDPD(int months) {
        int dpd = 0;
        try {
            Date reportDate = getReportDate();
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isObject()) {
                dpd = maxDPDLastXmonths(accountDetails, months, reportDate);
            } else if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode caisAccountDetails : accountDetails) {
                    dpd = max(dpd, maxDPDLastXmonths(caisAccountDetails, months, reportDate));
                }
            }
        } catch (Exception e) {
            logger.info("Exception while getting max DPD from experian", e);
        }
        return dpd;
    }

    @Override
    public boolean writtenOffLast12Months() {
        boolean writtenOff = false;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (jsonNode.get(ExperianConstants.ACCT_STATUS) != null && writtenOffAccStatus.contains(jsonNode.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        writtenOff = true;
                        break;
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_STATUS) != null && writtenOffAccStatus.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    writtenOff = true;
                }
            }
        } catch (Exception e) {
            logger.error("Exception in checking written off in experian", e);
        }
        return writtenOff;
    }

    @Override
    public int getMaxOverdueAmount() {
        int max = 0;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (jsonNode.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !jsonNode.get(ExperianConstants.AMOUNT_PAST_DUE).toString().equalsIgnoreCase("\"\"")) {
                        max = max(max, jsonNode.get(ExperianConstants.AMOUNT_PAST_DUE).asInt());
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).toString().equalsIgnoreCase("\"\"")) {
                    max = max(max, accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).asInt());
                }
            }
        } catch (Exception e) {
            logger.error("Exception in checking max due amount in experian", e);
        }
        return max;
    }

    @Override
    public int countUnsecuredLoanEnquiries(int months) {
        int count = 0;
        try {
            Date reportDate = getReportDate();
            Calendar c = Calendar.getInstance();
            c.setTime(reportDate);
            c.add(Calendar.MONTH, -months);
            String month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1) : (c.get(Calendar.MONTH) + 1) + "";
            String day = (c.get(Calendar.DAY_OF_MONTH) + 1) < 10 ? "0" + (c.get(Calendar.DAY_OF_MONTH) + 1) : (c.get(Calendar.DAY_OF_MONTH) + 1) + "";
            long previousXMonthDate = Long.parseLong(c.get(Calendar.YEAR) + month + day);
            if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null
                    && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isObject()) {
                JsonNode jsonNode = response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS);
                if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null
                        && jsonNode.get(ExperianConstants.DOR) != null
                        && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                        && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                    count++;
                }
            } else if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null
                    && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isArray()) {
                for (JsonNode jsonNode : response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS)) {
                    if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null
                            && unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                            && jsonNode.get(ExperianConstants.DOR) != null
                            && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiries in experian", e);
        }
        return count;
    }

    @Override
    public int countSecuredLoanEnquiries(int months) {
        int count = 0;
        try {
            Date reportDate = getReportDate();
            Calendar c = Calendar.getInstance();
            c.setTime(reportDate);
            c.add(Calendar.MONTH, -months);
            String month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1) : (c.get(Calendar.MONTH) + 1) + "";
            String day = (c.get(Calendar.DAY_OF_MONTH) + 1) < 10 ? "0" + (c.get(Calendar.DAY_OF_MONTH) + 1) : (c.get(Calendar.DAY_OF_MONTH) + 1) + "";
            long previousXMonthDate = Long.parseLong(c.get(Calendar.YEAR) + month + day);
            if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null
                    && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isObject()) {
                JsonNode jsonNode = response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS);
                if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null
                        && jsonNode.get(ExperianConstants.DOR) != null
                        && !unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                        && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                    count++;
                }
            } else if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null
                    && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isArray()) {
                for (JsonNode jsonNode : response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS)) {
                    if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null
                            && !unsecuredLoan.contains(jsonNode.get(ExperianConstants.ENQUIRY_REASON).asInt())
                            && jsonNode.get(ExperianConstants.DOR) != null
                            && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiries in experian", e);
        }
        return count;
    }

    @Override
    public double maxCurrentBalance(BureauLoanType loanType) {
        double max = 0D;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return max;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            max = max(max, getCurrentBalance(jsonNode));
                        } else if (getCurrentBalance(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {
                            max = max(max, getCurrentBalance(jsonNode));
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        max = max(max, getCurrentBalance(accountDetails));
                    } else if (getCurrentBalance(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {
                        max = max(max, getCurrentBalance(accountDetails));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxLoanAmount in experian", e);
        }
        return max;
    }

    @Override
    public double maxLoanAmount(BureauLoanType loanType) {
        double max = 0D;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return max;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            max = max(max, getLoanAmount(jsonNode));
                        } else if (getLoanAmount(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {
                            max = max(max, getLoanAmount(jsonNode));
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        max = max(max, getLoanAmount(accountDetails));
                    } else if (getLoanAmount(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {
                        max = max(max, getLoanAmount(accountDetails));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxLoanAmount in experian", e);
        }
        return max;
    }

    @Override
    public double minLoanAmount(BureauLoanType loanType) {
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return 0;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                double min = Double.MAX_VALUE;
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            min = Math.min(min, getLoanAmount(jsonNode));
                        } else if (getLoanAmount(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {
                            min = Math.min(min, getLoanAmount(jsonNode));
                        }
                    }
                }
                return min == Double.MAX_VALUE ? 0 : min;
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        return getLoanAmount(accountDetails);
                    } else if (getLoanAmount(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {
                        return getLoanAmount(accountDetails);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in minLoanAmount in experian", e);
        }
        return 0;
    }

    @Override
    public double totalLoanAmount(BureauLoanType loanType) {
        double total = 0D;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return total;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            total += getLoanAmount(jsonNode);
                        } else if (getLoanAmount(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {
                            total += getLoanAmount(jsonNode);
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        total += getLoanAmount(accountDetails);
                    } else if (getLoanAmount(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {
                        total += getLoanAmount(accountDetails);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in totalLoanAmount in experian", e);
        }
        return total;
    }

    @Override
    public Integer getVintage(BureauLoanType loanType) {
        Integer vintage = null;
        DateTime min = new DateTime();
        boolean loanFound = false;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return vintage;
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            try {
                                DateTime openDate = formatter.parseDateTime(jsonNode.get(ExperianConstants.OPEN_DATE).toString());
                                min = openDate.isBefore(min) ? openDate : min;
                                loanFound = true;
                            } catch (Exception e) {
                                logger.info("Invalid Open_Date");
                            }
                        } else if (getLoanAmount(jsonNode) >= 10000) {
                            try {
                                DateTime openDate = formatter.parseDateTime(jsonNode.get(ExperianConstants.OPEN_DATE).toString());
                                min = openDate.isBefore(min) ? openDate : min;
                                loanFound = true;
                            } catch (Exception e) {
                                logger.info("Invalid Open_Date");
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        try {
                            DateTime openDate = formatter.parseDateTime(accountDetails.get(ExperianConstants.OPEN_DATE).toString());
                            min = openDate.isBefore(min) ? openDate : min;
                            loanFound = true;
                        } catch (Exception e) {
                            logger.info("Invalid Open_Date");
                        }
                    } else if (getLoanAmount(accountDetails) >= 10000) {
                        try {
                            DateTime openDate = formatter.parseDateTime(accountDetails.get(ExperianConstants.OPEN_DATE).toString());
                            min = openDate.isBefore(min) ? openDate : min;
                            loanFound = true;
                        } catch (Exception e) {
                            logger.info("Invalid Open_Date");
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getVintage in experian", e);
        }
        if (loanFound) {
            vintage = Days.daysBetween(min, DateTime.now()).getDays();
        }
        return vintage;
    }

    @Override
    public int getTotalLoanCount() {
        int count = 0;
        try {
            List<Integer> accountTypes = ExperianConstants.LOAN_TYPE_ACC.get(BureauLoanType.CREDIT_CARD);
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode jsonNode : accountDetails) {
                    if (!accountTypes.contains(jsonNode.get(ExperianConstants.ACCT_TYPE).asInt())) {//not a credit card
                        count++;
                    } else if (getLoanAmount(jsonNode) >= 10000 && !isLoanClosed(jsonNode)) {//credit card with amount>=10k and active
                        count++;
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (!accountTypes.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt())) {//not a credit card
                    count++;
                } else if (getLoanAmount(accountDetails) >= 10000 && !isLoanClosed(accountDetails)) {//credit card with amount>=10k and active
                    count++;
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getTotalLoanCount in experian", e);
        }
        return count;
    }

    @Override
    public String getPancard() {
        try {
            String pancard = null;
            JsonNode currentApplicationDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CURRENT_APPLICATION).get(ExperianConstants.CURRENT_APPLICATION_DETAILS);
            if (currentApplicationDetails != null && currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS) != null) {
                pancard = currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS).get(ExperianConstants.INCOME_TAX_PAN).asText();
            }
            if (!StringUtils.isEmpty(pancard) && !"null".equalsIgnoreCase(pancard)) {
                return pancard;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while fetching pancard from experian", e);
        }
        return null;
    }

    @Override
    public List<String> getAddress() {
        List<String> addressList = null;
        if (Objects.isNull(response)) {
            return addressList;
        }
        try {
            if (Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE))
                    || Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT))) {
                return addressList;
            }
            JsonNode caisAccountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);

            List<JsonNode> accountDetails = CommonUtil.jsonNodeArrayUtil(caisAccountDetails);
            List<JsonNode> addressDetails = new ArrayList<>();
            for (JsonNode accountDetail : accountDetails) {
                addressDetails.addAll(CommonUtil.jsonNodeArrayUtil(accountDetail.get(ExperianConstants.CAIS_HOLDER_ADDRESS_DETAILS)));
            }
            if (!addressDetails.isEmpty()) {
                for (JsonNode addressDetail : addressDetails) {
                    String address = "";
                    String firstLine = addressDetail.get(ExperianConstants.FIRST_LINE_OF_ADDRESS).asText();
                    String secondLine = addressDetail.get(ExperianConstants.SECOND_LINE_OF_ADDRESS).asText();
                    String thirdLine = addressDetail.get(ExperianConstants.THIRD_LINE_OF_ADDRESS).asText();
                    String city = addressDetail.get(ExperianConstants.CITY).asText();
                    String pincode = addressDetail.get(ExperianConstants.POSTAL_CODE).asText();
                    if (!StringUtils.isEmpty(firstLine) && !"null".equalsIgnoreCase(firstLine)) {
                        address += firstLine;
                        if (!StringUtils.isEmpty(secondLine) && !"null".equalsIgnoreCase(secondLine)) {
                            address += "," + secondLine;
                        }
                        if (!StringUtils.isEmpty(thirdLine) && !"null".equalsIgnoreCase(thirdLine)) {
                            address += "," + thirdLine;
                        }
                        if (!StringUtils.isEmpty(city) && !"null".equalsIgnoreCase(city)) {
                            address += "," + city;
                        }
                        if (!StringUtils.isEmpty(pincode) && !"null".equalsIgnoreCase(pincode)) {
                            address += "," + pincode;
                        }
                        if (addressList == null) {
                            addressList = new ArrayList<>();
                        }
                        addressList.add(address);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching address from experian", e);
        }
        return addressList;
    }

    @Override
    public Gender getGender() {
        try {
            int genderCode = 0;
            if (Objects.isNull(response)) {
                return null;
            }
            if (Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE)) ||
                    Objects.isNull(response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CURRENT_APPLICATION))) {
                return null;
            }
            JsonNode currentApplicationDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CURRENT_APPLICATION).get(ExperianConstants.CURRENT_APPLICATION_DETAILS);
            if (currentApplicationDetails != null && currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS) != null) {
                genderCode = currentApplicationDetails.get(ExperianConstants.CURRENT_APPLICANT_DETAILS).get(ExperianConstants.GENDER_CODE).asInt();
            }
            if (genderCode == 1) {
                return Gender.FEMALE;
            } else if (genderCode == 2) {
                return Gender.MALE;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception while fetching gender from experian", e);
        }
        return null;
    }

    @Override
    public int subDPDMoreThan(int maxDpd, int month) {
        int count = 0;
        int dpd = 0;
        Date dateReported = null;
        Date reportDate = getReportDate();
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                    count++;
                                }
                                if (count > 1) {
                                    count = 1;
                                    dpd = count + dpd;
                                }
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                    dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                count++;
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching tradeDpd in postPe :{}", e);
        }

        return dpd;
    }

    @Override
    public int subDPDLessThan(int maxDpd, int month) {
        int count = 0;
        int dpd = 0;
        Date dateReported = null;
        Date reportDate = getReportDate();
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                    count++;
                                }
                                if (count > 1) {
                                    count = 1;
                                    dpd = count + dpd;
                                }
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                    dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() >= maxDpd || (!history.get(ExperianConstants.ASSET_CLASSIFICATION).asText().equalsIgnoreCase("?") && assetList.contains(history.get(ExperianConstants.ASSET_CLASSIFICATION)))) {
                                count++;
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching tradeDpd in postPe :{}", e);
        }

        return dpd;
    }

    @Override
    public int tradeDpdLessThan(int minDpd, int maxDpd, int month) {
        int count = 0;
        int dpd = 0;
        Date dateReported = null;
        Date reportDate = getReportDate();
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= minDpd && history.get(ExperianConstants.DPD).asInt() <= maxDpd) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                    count++;
                                }
                                if (count > 1) {
                                    count = 1;
                                    dpd = count + dpd;
                                }
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                    dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                count++;
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching tradeDpd in postPe :{}", e);
        }

        return dpd;
    }

    @Override
    public int tradeDpdMoreThan(int minDpd, int maxDpd, int month) {
        int count = 0;
        int dpd = 0;
        Date dateReported = null;
        Date reportDate = getReportDate();
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= minDpd && history.get(ExperianConstants.DPD).asInt() <= maxDpd) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                    count++;
                                }
                                if (count > 1) {
                                    count = 1;
                                    dpd = count + dpd;
                                }
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                    dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() > minDpd && history.get(ExperianConstants.DPD).asInt() < maxDpd) {
                                count++;
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching tradeDpd in postPe :{}", e);
        }

        return dpd;
    }

    private int maxDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        int max = 0;
        Date dateReported = null;
        try {
            if (jsonNode.get(ExperianConstants.DATE_REPORTED) != null
                    && !jsonNode.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                dateReported = dateFormat.parse(jsonNode.get(ExperianConstants.DATE_REPORTED).asText());
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        List<String> monthYear = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > months * 30) {
            return max;
        }
        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) <= months * 30) {
            c.setTime(dateReported);
        } else {
            c.setTime(reportDate);
        }
        String month;
        for (int i = 0; i < months; i++) {
            month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1)
                    : (c.get(Calendar.MONTH) + 1) + "";
            monthYear.add(month + "$" + c.get(Calendar.YEAR));// 01$2020
            c.add(Calendar.MONTH, -1);
        }
        if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isArray()) {
            for (JsonNode history : jsonNode.get(ExperianConstants.ACCT_HISTORY)) {
                if (monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                        && !history.get(ExperianConstants.DPD).isNull()
                        && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                        && history.get(ExperianConstants.DPD).asInt() >= 0) {
                    max = max(max, history.get(ExperianConstants.DPD).asInt());
                }
            }
        } else if (jsonNode.get(ExperianConstants.ACCT_HISTORY) != null
                && jsonNode.get(ExperianConstants.ACCT_HISTORY).isObject()) {
            JsonNode history = jsonNode.get(ExperianConstants.ACCT_HISTORY);
            if (monthYear.contains(history.get("Month").asText() + "$" + history.get("Year").asText())
                    && !history.get(ExperianConstants.DPD).isNull()
                    && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")
                    && history.get(ExperianConstants.DPD).asInt() >= 0) {
                max = max(max, history.get(ExperianConstants.DPD).asInt());
            }
        }
        return max;
    }

    public static int historyScore(int creditHistory) {
        if (creditHistory < 6) {
            return 0;
        } else if (creditHistory < 12) {
            return 1;
        } else if (creditHistory < 24) {
            return 2;
        } else
            return 3;
    }

    public static int unsecuredLoanScore(double unsecuredLoanRatio6mon) {
        if (unsecuredLoanRatio6mon < 0.17) {
            return 3;
        } else if (unsecuredLoanRatio6mon < 0.5) {
            return 2;
        } else if (unsecuredLoanRatio6mon < 0.67) {
            return 1;
        } else
            return 0;
    }

    public static int loanTypesScore(int typesOfLoan) {
        if (typesOfLoan < 3) {
            return 0;
        } else if (typesOfLoan < 5) {
            return 1;
        } else if (typesOfLoan < 8) {
            return 2;
        } else
            return 3;
    }

    public static int loanSanctionedScore(int loanSanctioned3mon) {
        if (loanSanctioned3mon < 2) {
            return 3;
        } else if (loanSanctioned3mon < 4) {
            return 2;
        } else if (loanSanctioned3mon < 6) {
            return 0;
        } else
            return -1;
    }

    public static int loanEnquiresScore(int loanEnquires3mon) {
        if (loanEnquires3mon < 4) {
            return 3;
        } else if (loanEnquires3mon < 7) {
            return 2;
        } else if (loanEnquires3mon < 10) {
            return 1;
        } else
            return 0;
    }

    public static int delinquencyScore(int delinquencyCount6mon) {
        if (delinquencyCount6mon < 1) {
            return 3;
        } else if (delinquencyCount6mon < 3) {
            return -1;
        } else if (delinquencyCount6mon < 6) {
            return -2;
        } else
            return -5;
    }

    public CreditScoreReportDetailDTO getCreditDetailReport(JsonNode bureauResponse) {

        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();

        try {
            CreditScoreReportDetailDTO.CreditCardUtilization creditCardUtilization = getCreditCardUtilization(bureauResponse);
            CreditScoreReportDetailDTO.PaymentHistory paymentHistory = getPaymentHistory(bureauResponse);
            CreditScoreReportDetailDTO.AgeOfAccount ageOfAccount = getAgeOfAccount(bureauResponse);
            CreditScoreReportDetailDTO.TotalAccount totalAccount = getTotalAccount(bureauResponse);
            CreditScoreReportDetailDTO.CreditEnquries creditEnquries = getCreditEnquiries(bureauResponse);

            creditScoreReportDetailDTO.setCreditEnquries(creditEnquries);
            creditScoreReportDetailDTO.setCreditCardUtilization(creditCardUtilization);
            creditScoreReportDetailDTO.setAgeOfAccount(ageOfAccount);
            creditScoreReportDetailDTO.setTotalAccount(totalAccount);
            creditScoreReportDetailDTO.setPaymentHistory(paymentHistory);
            creditScoreReportDetailDTO.setExperianNumber(getExperianNumber(bureauResponse));
        } catch (Exception ex) {
            logger.error("Error Occurred while checking loan and credit details, Error :{0}", ex);
        }


        return creditScoreReportDetailDTO;
    }

    public CreditScoreReportDetailDTO.CreditCardUtilization getCreditCardUtilization(JsonNode bureauResponse) {

        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        CreditScoreReportDetailDTO.CreditCardUtilization creditCardUtilization = creditScoreReportDetailDTO.new CreditCardUtilization();
        try {
            boolean cardUtilizationUtilizationCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS));
            int cardLimit = 0;
            int currentBalance = 0;
            int totalUtilization = 0;
            int limit = 0;
            String impact = null;

            if (cardUtilizationUtilizationCheck) {
                JsonNode caisAccountDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);

                if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isObject() && caisAccountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 10 && !isLoanClosed(caisAccountDetails)) {
                    if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount"))) {
                        cardLimit = caisAccountDetails.get("Credit_Limit_Amount").asInt();
                    }
                    if (Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                        cardLimit = caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt();
                    }
                    if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                        cardLimit = max(caisAccountDetails.get("Credit_Limit_Amount").asInt(), caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt());
                    }
                    currentBalance = max(caisAccountDetails.get("Current_Balance").asInt(), 0);


                } else if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isArray()) {
                    for (JsonNode caisAccountDetail : caisAccountDetails) {
                        if (caisAccountDetail.get(ExperianConstants.ACCT_TYPE).asInt() == 10 && !isLoanClosed(caisAccountDetail)) {
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount"))) {
                                limit = caisAccountDetail.get("Credit_Limit_Amount").asInt();
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                limit = caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt();
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                limit = max(caisAccountDetail.get("Credit_Limit_Amount").asInt(), caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt());
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Current_Balance"))) {
                                currentBalance += caisAccountDetail.get("Current_Balance").asInt();
                            }
                            cardLimit += limit;
                        }
                    }
                }
            }
            if (cardLimit != 0) {
                totalUtilization = (currentBalance * 100) / cardLimit;
            }

            if (totalUtilization > 100) {
                totalUtilization = 100;
            }

            if (totalUtilization < 25) {
                impact = "excellent";
            } else if (totalUtilization < 75) {
                impact = "average";
            } else {
                impact = "bad";
            }
            if (cardLimit == 0 && currentBalance == 0 && totalUtilization == 0) {
                return null;
            }
            creditCardUtilization.setCardUtilization(currentBalance);
            creditCardUtilization.setCardLimit(cardLimit);
            creditCardUtilization.setTotalUtilization(totalUtilization);
            creditCardUtilization.setImpact(impact);
            return creditCardUtilization;
        } catch (Exception ex) {
            logger.error("Error Occurred while calculating card utilization Error :{0}", ex);
        }

        return null;
    }

    public CreditScoreReportDetailDTO.PaymentHistory getPaymentHistory(JsonNode bureauResponse) {

        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        CreditScoreReportDetailDTO.PaymentHistory paymentHistory = creditScoreReportDetailDTO.new PaymentHistory();

        try {
            boolean paymentHistoryCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS));
            int totalPayment = 0;
            int onTimePayment = 0;
            int deqPayment = 0;
            int timelyPayment = 0;
            String impact = null;

            if (paymentHistoryCheck) {
                JsonNode caisAccountDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
                if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isObject()) {
                    JsonNode caisAccountHistories = caisAccountDetails.get(ExperianConstants.ACCT_HISTORY);
                    if (Objects.nonNull(caisAccountHistories)) {
                        totalPayment = caisAccountHistories.size();
                        for (JsonNode caisAccountHistory : caisAccountHistories) {
                            if (Objects.isNull(caisAccountHistory.get(ExperianConstants.DPD)) || caisAccountHistory.get(ExperianConstants.DPD).intValue() == 0) {
                                onTimePayment += 1;
                            } else if (Objects.nonNull(caisAccountHistory.get(ExperianConstants.DPD)) || caisAccountHistory.get(ExperianConstants.DPD).intValue() != 0) {
                                deqPayment += 1;
                            }
                        }
                    }
                } else if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isArray()) {
                    for (JsonNode caisAccountDetail : caisAccountDetails) {
                        JsonNode caisAccountHistories = caisAccountDetail.get(ExperianConstants.ACCT_HISTORY);
                        totalPayment += caisAccountHistories.size();
                        for (JsonNode caisAccountHistory : caisAccountHistories) {
                            if (Objects.isNull(caisAccountHistory.get(ExperianConstants.DPD)) || caisAccountHistory.get(ExperianConstants.DPD).intValue() == 0) {
                                onTimePayment += 1;
                            } else if (Objects.nonNull(caisAccountHistory.get(ExperianConstants.DPD)) || caisAccountHistory.get(ExperianConstants.DPD).intValue() != 0) {
                                deqPayment += 1;
                            }
                        }
                    }
                }
                timelyPayment = (onTimePayment * 100) / totalPayment;
            }

            if (timelyPayment > 90) {
                impact = "excellent";
            } else if (timelyPayment > 50 && timelyPayment <= 90) {
                impact = "average";
            } else if (timelyPayment <= 50) {
                impact = "bad";
            }

            if (totalPayment == 0 && onTimePayment == 0 && timelyPayment == 0) {
                return null;
            }
            paymentHistory.setTotalPayment(totalPayment);
            paymentHistory.setOntimePayment(onTimePayment);
            paymentHistory.setTimelyPayment(timelyPayment);
            paymentHistory.setImpact(impact);
            return paymentHistory;
        } catch (Exception ex) {
            logger.error("Error Occurred while payment history, Error :{0}", ex);
        }

        return null;
    }

    public CreditScoreReportDetailDTO.AgeOfAccount getAgeOfAccount(JsonNode bureauResponse) {

        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        CreditScoreReportDetailDTO.AgeOfAccount ageOfAccount = creditScoreReportDetailDTO.new AgeOfAccount();

        try {
            boolean ageOfAccountCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS));
            int averageAge = 0;
            int newestAccount = 0;
            int oldestAccount = 0;
            int currentDiff = 0;
            int total = 0;
            String impact = null;

            if (ageOfAccountCheck) {
                JsonNode caisAccountDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
                if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isObject()) {
                    JsonNode openDate = caisAccountDetails.get(ExperianConstants.OPEN_DATE) == null ? caisAccountDetails.get(ExperianConstants.DATE_ADDITION) : caisAccountDetails.get(ExperianConstants.OPEN_DATE);
                    Date dateReported = null;
                    Date OpenDateType = null;
                    try {
                        if (caisAccountDetails.get(ExperianConstants.DATE_REPORTED) != null
                                && !caisAccountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("") && Objects.nonNull(openDate) && !openDate.asText().equalsIgnoreCase("")) {
                            dateReported = dateFormat.parse(caisAccountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                            OpenDateType = dateFormat.parse(openDate.asText());

                            averageAge =
                                    (int) CommonUtil.getDateDiffInDays(OpenDateType, dateReported) / 365;
                            newestAccount = averageAge;
                            oldestAccount = averageAge;
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                } else if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isArray()) {
                    total = caisAccountDetails.size();
                    for (JsonNode caisAccountDetail : caisAccountDetails) {
                        JsonNode openDate = caisAccountDetail.get(ExperianConstants.OPEN_DATE) == null ? caisAccountDetail.get(ExperianConstants.DATE_ADDITION) : caisAccountDetail.get(ExperianConstants.OPEN_DATE);
                        Date dateReported = null;
                        Date OpenDateType = null;
                        try {
                            if (caisAccountDetail.get(ExperianConstants.DATE_REPORTED) != null
                                    && !caisAccountDetail.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("") && Objects.nonNull(openDate) && !openDate.asText().equalsIgnoreCase("")) {
                                dateReported = dateFormat.parse(caisAccountDetail.get(ExperianConstants.DATE_REPORTED).asText());
                                OpenDateType = dateFormat.parse(openDate.asText());

                                currentDiff = (int) CommonUtil.getDateDiffInDays(OpenDateType,
                                        dateReported) / 365;
                                newestAccount = Math.min(newestAccount == 0 ? Integer.MAX_VALUE : newestAccount, currentDiff);
                                oldestAccount = max(oldestAccount, currentDiff);
                                averageAge += currentDiff;
                            }
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                        }
                    }
                    averageAge = averageAge / total;
                }
            }
            if (averageAge < 1) {
                impact = "average";
            } else {
                impact = "excellent";
            }

            if (newestAccount == 0 && oldestAccount == 0 && averageAge == 0) {
                return null;
            }

            ageOfAccount.setNewestAccount(newestAccount);
            ageOfAccount.setOldestAccount(oldestAccount);
            ageOfAccount.setAverageAge(averageAge);
            ageOfAccount.setImpact(impact);

            return ageOfAccount;
        } catch (Exception ex) {
            logger.error("Error Occurred while checking age of account, Error :{0}", ex);
        }
        return null;
    }

    public CreditScoreReportDetailDTO.TotalAccount getTotalAccount(JsonNode bureauResponse) {
        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        CreditScoreReportDetailDTO.TotalAccount totalAccount = creditScoreReportDetailDTO.new TotalAccount();

        try {
            boolean totalAccountCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS));

            int totalNumAccount = 0;
            int activeAccount = 0;
            int closedAccount = 0;
            String impact = null;

            if (totalAccountCheck) {
                JsonNode caisAccountDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
                if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isObject()) {
                    if (isLoanClosed(caisAccountDetails)) {
                        closedAccount += 1;
                    } else {
                        activeAccount += 1;
                    }
                    totalNumAccount = closedAccount + activeAccount;
                } else if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isArray()) {
                    for (JsonNode caisAccountDetail : caisAccountDetails) {
                        if (isLoanClosed(caisAccountDetail)) {
                            closedAccount += 1;
                        } else {
                            activeAccount += 1;
                        }
                    }
                    totalNumAccount = closedAccount + activeAccount;
                }
            }

            if (activeAccount <= 10) {
                impact = "excellent";
            } else if (activeAccount <= 20) {
                impact = "average";
            } else {
                impact = "bad";
            }

            if (totalNumAccount == 0 && activeAccount == 0 && closedAccount == 0) {
                return null;
            }

            totalAccount.setTotalAccount(totalNumAccount);
            totalAccount.setActiveAccount(activeAccount);
            totalAccount.setClosedAccount(closedAccount);
            totalAccount.setImpact(impact);
            return totalAccount;
        } catch (Exception ex) {
            logger.error("Error Occurred while checking total account, Error :{0}", ex);
        }
        return null;
    }

    public CreditScoreReportDetailDTO.CreditEnquries getCreditEnquiries(JsonNode bureauResponse) {
        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        CreditScoreReportDetailDTO.CreditEnquries creditEnquries = creditScoreReportDetailDTO.new CreditEnquries();

        try {
            boolean creditEnquriesCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get("Current_Application")) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get("Current_Application").get("Current_Application_Details"));

            int totalEnquries = 0;
            int creditCardEnquries = 0;
            int loanEnqueries = 0;
            String impact = null;

            if (creditEnquriesCheck) {
                JsonNode currentApplicationDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get("Current_Application").get("Current_Application_Details");
                if (currentApplicationDetails.isObject()) {
                    JsonNode enquiryReason = currentApplicationDetails.get("Enquiry_Reason");
                    if (enquiryReason.asText().equals("7")) {
                        creditCardEnquries += 1;
                    } else {
                        loanEnqueries += 1;
                    }
                } else if (currentApplicationDetails.isArray()) {
                    for (JsonNode currentApplicationDetail : currentApplicationDetails) {
                        JsonNode enquiryReason = currentApplicationDetail.get("Enquiry_Reason");
                        if (enquiryReason.asInt() == 7) {
                            creditCardEnquries += 1;
                        } else {
                            loanEnqueries += 1;
                        }
                    }
                }

                if (bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY) != null
                        && bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY)
                        .get("TotalCAPSLast180Days") != null) {
                    totalEnquries = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.CAPS_SUMMARY)
                            .get("TotalCAPSLast180Days").asInt();
                }
                totalEnquries = max(totalEnquries, creditCardEnquries + loanEnqueries);
            }


            if (totalEnquries <= 2) {
                impact = "excellent";
            } else if (totalEnquries > 3 && totalEnquries <= 6) {
                impact = "average";
            } else {
                impact = "bad";
            }

            if (totalEnquries == 0 && loanEnqueries == 0 && creditCardEnquries == 0) {
                return null;
            }

            creditEnquries.setTotalEnquiries(totalEnquries);
            creditEnquries.setLoanEnquiries(loanEnqueries);
            creditEnquries.setCreditCardEnquiries(creditCardEnquries);
            creditEnquries.setImpact(impact);
            return creditEnquries;
        } catch (Exception ex) {
            logger.error("Error Occurred while checking credit enquries, Error :{0}", ex);
        }
        return null;
    }

    public String getExperianNumber(JsonNode bureauResponse) {
        String experianNumber = null;
        boolean experianHeaderDetails = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get("CreditProfileHeader"));
        if (experianHeaderDetails) {
            JsonNode headerDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get("CreditProfileHeader");
            if (Objects.nonNull(headerDetails.get("ReportNumber"))) {
                experianNumber = headerDetails.get("ReportNumber").asText();
            }
        }

        return experianNumber;
    }

    public LoanAndCreditCardDetailDTO getLoanAndCreditDetail(JsonNode bureauResponse) {

        LoanAndCreditCardDetailDTO loanAndCreditCardDetailDTO = new LoanAndCreditCardDetailDTO();
        LoanAndCreditCardDetailDTO.CreditCardDetail creditCardDetail = loanAndCreditCardDetailDTO.new CreditCardDetail();
        LoanAndCreditCardDetailDTO.LoanDetail loanDetail = loanAndCreditCardDetailDTO.new LoanDetail();

        List<LoanAndCreditCardDetailDTO.CreditCardDetail> creditCardDetails = new ArrayList<>();
        List<LoanAndCreditCardDetailDTO.LoanDetail> loanDetails = new ArrayList<>();

        try {
            boolean totalAccountCheck = Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT)) && Objects.nonNull(bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS));
            int senctionedAmount = 0;

            if (totalAccountCheck) {
                JsonNode caisAccountDetails = bureauResponse.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
                if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isObject()) {
                    if (caisAccountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 10) {
                        creditCardDetail.setBankName(caisAccountDetails.get("Subscriber_Name").asText());
                        creditCardDetail.setStatus(!isLoanClosed(caisAccountDetails));
                        creditCardDetail.setCreditCardNumber(caisAccountDetails.get("Account_Number").asText());
                        if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount"))) {
                            creditCardDetail.setCardLimit(max(caisAccountDetails.get("Credit_Limit_Amount").asInt(), 0));
                        }
                        if (Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                            creditCardDetail.setCardLimit(max(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt(), 0));
                        }
                        if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                            creditCardDetail.setCardLimit(max(caisAccountDetails.get("Credit_Limit_Amount").asInt(), caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt()));
                        }
                        creditCardDetail.setBalance(caisAccountDetails.get("Current_Balance").asInt());

                        creditCardDetails.add(creditCardDetail);
                    } else {
                        if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount"))) {
                            senctionedAmount = caisAccountDetails.get("Credit_Limit_Amount").asInt();
                        }
                        if (Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                            senctionedAmount = caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt();
                        }
                        if (Objects.nonNull(caisAccountDetails.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount"))) {
                            senctionedAmount = max(caisAccountDetails.get("Credit_Limit_Amount").asInt(), caisAccountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt());
                        }
                        loanDetail.setAccountNumber(caisAccountDetails.get("Account_Number").asText());
                        loanDetail.setBankName(caisAccountDetails.get("Subscriber_Name").asText());
                        loanDetail.setSanctionedAmount(senctionedAmount);
                        loanDetail.setTenure(caisAccountDetails.get("Repayment_Tenure").asText());
                        loanDetail.setStatus(!isLoanClosed(caisAccountDetails));
                        loanDetail.setCurrentBalance(caisAccountDetails.get("Current_Balance").asText());
                        loanDetail.setRateOfInterest(caisAccountDetails.get("Rate_of_Interest").asText());
                        loanDetails.add(loanDetail);
                    }
                } else if (Objects.nonNull(caisAccountDetails) && caisAccountDetails.isArray()) {
                    for (JsonNode caisAccountDetail : caisAccountDetails) {
                        creditCardDetail = loanAndCreditCardDetailDTO.new CreditCardDetail();
                        loanDetail = loanAndCreditCardDetailDTO.new LoanDetail();
                        if (caisAccountDetail.get(ExperianConstants.ACCT_TYPE).asInt() == 10) {
                            creditCardDetail.setBankName(caisAccountDetail.get("Subscriber_Name").asText());

                            creditCardDetail.setStatus(!isLoanClosed(caisAccountDetail));
                            creditCardDetail.setCreditCardNumber(caisAccountDetail.get("Account_Number").asText());
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount"))) {
                                creditCardDetail.setCardLimit(max(caisAccountDetail.get("Credit_Limit_Amount").asInt(), 0));
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                creditCardDetail.setCardLimit(max(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt(), 0));
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                creditCardDetail.setCardLimit(max(caisAccountDetail.get("Credit_Limit_Amount").asInt(), caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt()));
                            }
                            creditCardDetail.setBalance(max(caisAccountDetail.get("Current_Balance").asInt(), 0));


                            creditCardDetails.add(creditCardDetail);
                        } else {
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount"))) {
                                senctionedAmount = caisAccountDetail.get("Credit_Limit_Amount").asInt();
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                senctionedAmount = caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt();
                            }
                            if (Objects.nonNull(caisAccountDetail.get("Credit_Limit_Amount")) && Objects.nonNull(caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount"))) {
                                senctionedAmount = max(caisAccountDetail.get("Credit_Limit_Amount").asInt(), caisAccountDetail.get("Highest_Credit_or_Original_Loan_Amount").asInt());
                            }
                            loanDetail.setAccountNumber(caisAccountDetail.get("Account_Number").asText());
                            loanDetail.setStatus(!isLoanClosed(caisAccountDetail));
                            loanDetail.setBankName(caisAccountDetail.get("Subscriber_Name").asText());
                            loanDetail.setSanctionedAmount(senctionedAmount);
                            loanDetail.setTenure(caisAccountDetail.get("Repayment_Tenure").asText());
                            loanDetail.setCurrentBalance(caisAccountDetail.get("Current_Balance").asText());
                            loanDetail.setRateOfInterest(caisAccountDetail.get("Rate_of_Interest").asText());
                            loanDetails.add(loanDetail);
                        }
                    }
                }
            }
            if (!loanDetails.isEmpty()) {
                loanAndCreditCardDetailDTO.setLoanDetail(loanDetails);
            }
            if (!creditCardDetails.isEmpty()) {
                loanAndCreditCardDetailDTO.setCreditCardDetail(creditCardDetails);
            }
            loanAndCreditCardDetailDTO.setExperianNumber(getExperianNumber(bureauResponse));

        } catch (Exception ex) {
            logger.error("Error Occurred while checking loan and credit details, Error :{0}", ex);
        }

        return loanAndCreditCardDetailDTO;
    }

    @Override
    public double unsecuredLoanUtilization() {
        Double currentBalance = 0D;
        Double sanctionAmount = 0D;
        Double utilization = 0D;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (unsecuredLoan.contains(accounts.get(ExperianConstants.ACCT_TYPE).asInt()) && activeStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        if (!accounts.get(ExperianConstants.CURRENT_BALANCE).asText().equalsIgnoreCase("") && accounts.get(ExperianConstants.CURRENT_BALANCE).asInt() > 0) {
                            if (Objects.nonNull(accounts.get("Highest_Credit_or_Original_Loan_Amount")) && !accounts.get("Highest_Credit_or_Original_Loan_Amount").asText().equalsIgnoreCase("") && accounts.get("Highest_Credit_or_Original_Loan_Amount").asInt() > 0) {
                                sanctionAmount = sanctionAmount + accounts.get("Highest_Credit_or_Original_Loan_Amount").doubleValue();
                            } else if (Objects.nonNull(accounts.get("Credit_Limit_Amount")) && !accounts.get("Credit_Limit_Amount").asText().equalsIgnoreCase("") && accounts.get("Credit_Limit_Amount").asInt() > 0) {
                                sanctionAmount = sanctionAmount + accounts.get("Credit_Limit_Amount").doubleValue();
                            } else {
                                sanctionAmount = sanctionAmount + accounts.get(ExperianConstants.CURRENT_BALANCE).doubleValue();
                            }
                            if (Objects.nonNull(accounts.get(ExperianConstants.CURRENT_BALANCE))) {
                                currentBalance = currentBalance + accounts.get(ExperianConstants.CURRENT_BALANCE).doubleValue();
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (unsecuredLoan.contains(accountDetails.get(ExperianConstants.ACCT_TYPE).asInt()) && activeStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    if (!accountDetails.get(ExperianConstants.CURRENT_BALANCE).asText().equalsIgnoreCase("") && accountDetails.get(ExperianConstants.CURRENT_BALANCE).asInt() > 0) {
                        if (Objects.nonNull(accountDetails.get("Highest_Credit_or_Original_Loan_Amount")) && !accountDetails.get("Highest_Credit_or_Original_Loan_Amount").asText().equalsIgnoreCase("") && accountDetails.get("Highest_Credit_or_Original_Loan_Amount").asInt() > 0) {
                            sanctionAmount = sanctionAmount + accountDetails.get("Highest_Credit_or_Original_Loan_Amount").doubleValue();
                        } else if (Objects.nonNull(accountDetails.get("Credit_Limit_Amount")) && !accountDetails.get("Credit_Limit_Amount").asText().equalsIgnoreCase("") && accountDetails.get("Credit_Limit_Amount").asInt() > 0) {
                            sanctionAmount = sanctionAmount + accountDetails.get("Credit_Limit_Amount").doubleValue();
                        } else {
                            sanctionAmount = sanctionAmount + accountDetails.get(ExperianConstants.CURRENT_BALANCE).doubleValue();
                        }
                        if (Objects.nonNull(accountDetails.get(ExperianConstants.CURRENT_BALANCE))) {
                            currentBalance = currentBalance + accountDetails.get(ExperianConstants.CURRENT_BALANCE).doubleValue();
                        }
                    }
                }
            }
            if (sanctionAmount != 0) {
                utilization = (currentBalance * 100) / sanctionAmount;
            }
            if (utilization > 100D) {
                utilization = 100D;
            }
        } catch (Exception e) {
            logger.error("Exception in unsecuredLoanUtilization", e);
        }
        return utilization;
    }

    @Override
    public Double nonCreditOverDuePastXMonthAndActive(int month) {
        Date dateReported = null;
        Date reportDate = getReportDate();
        Double pastDue = 0D;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() != 10 && activeStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                            dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                        }
                        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                            if (accounts.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accounts.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + accounts.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() != 10 && activeStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking Non Credit Card OverDue Past x:{} month", month, e);
        }
        return pastDue;
    }

    @Override
    public Double creditCardOverDuePastXMonth(int month) {
        Date dateReported = null;
        Double pastDue = 0D;
        Date reportDate = getReportDate();
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() == 10 && activeStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                            dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                        }
                        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                            if (accounts.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accounts.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + accounts.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 10 && activeStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Fetching ActiveCreditCard overdue past x:{} month,e:{}", month, e);
        }
        return pastDue;
    }

    @Override
    public Double closedLoanWithOverDueLastXMonth(int month) {
        Date dateClosed = null;
        Date reportDate = getReportDate();
        Double pastDue = 0D;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (closedStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS))) {
                        if (accounts.get(ExperianConstants.DATE_CLOSED) != null && !accounts.get(ExperianConstants.DATE_CLOSED).asText().equalsIgnoreCase("")) {
                            dateClosed = dateFormat.parse(accounts.get(ExperianConstants.DATE_CLOSED).asText());
                        }
                        if (dateClosed != null && CommonUtil.getDateDiffInDays(dateClosed, reportDate) < month * 30) {
                            if (accounts.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accounts.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + accounts.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (closedStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS))) {
                    if (accountDetails.get(ExperianConstants.DATE_CLOSED) != null && !accountDetails.get(ExperianConstants.DATE_CLOSED).asText().equalsIgnoreCase("")) {
                        dateClosed = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_CLOSED).asText());
                    }
                    if (dateClosed != null && CommonUtil.getDateDiffInDays(dateClosed, reportDate) < month * 30) {
                        if (accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking Non Credit Card OverDue Past x:{} month,e:{}", month, e);
        }
        return pastDue;
    }

    @Override
    public Double settleLoanPastXMonth(int month) {
        Date dateReported = null;
        Date reportDate = getReportDate();
        Double pastDue = 0D;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (!accounts.get(ExperianConstants.SETTLED_STATUS).asText().equalsIgnoreCase("") && writtenOffAccStatus.contains(accounts.get(ExperianConstants.SETTLED_STATUS))) {
                        if (accounts.get(ExperianConstants.DATE_REPORTED) != null && !accounts.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                            dateReported = dateFormat.parse(accounts.get(ExperianConstants.DATE_REPORTED).asText());
                        }
                        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                            if (accounts.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accounts.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + accounts.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (!accountDetails.get(ExperianConstants.SETTLED_STATUS).asText().equalsIgnoreCase("") && writtenOffAccStatus.contains(accountDetails.get(ExperianConstants.SETTLED_STATUS))) {
                    if (accountDetails.get(ExperianConstants.DATE_REPORTED) != null && !accountDetails.get(ExperianConstants.DATE_REPORTED).asText().equalsIgnoreCase("")) {
                        dateReported = dateFormat.parse(accountDetails.get(ExperianConstants.DATE_REPORTED).asText());
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE) != null && !accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accountDetails.get(ExperianConstants.AMOUNT_PAST_DUE).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking Non Credit Card OverDue Past x:{} month,e:{}", month, e);
        }
        return pastDue;
    }

    @Override
    public int maxCreditCardTradeMoreThan60(int maxDpd) {
        int dpd = 0;
        int count = 0;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() == 10) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                    count++;
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 10) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                count++;
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxCreditCardDPD", e);
        }
        return dpd;
    }

    @Override
    public int maxNonCreditCardTradeMoreThan60(int maxDpd) {
        int count = 0;
        int dpd = 0;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() != 10) {
                        if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                            for (JsonNode history : accounts.get(ExperianConstants.ACCT_HISTORY)) {
                                if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                    if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                        count++;
                                    }
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        } else if (accounts.get(ExperianConstants.ACCT_HISTORY) != null && accounts.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                            JsonNode history = accounts.get(ExperianConstants.ACCT_HISTORY);
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                    count++;
                                }
                            }
                            if (count > 1) {
                                count = 1;
                                dpd = count + dpd;
                            }
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() != 10) {
                    if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isArray()) {
                        for (JsonNode history : accountDetails.get(ExperianConstants.ACCT_HISTORY)) {
                            if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                                if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    } else if (accountDetails.get(ExperianConstants.ACCT_HISTORY) != null && accountDetails.get(ExperianConstants.ACCT_HISTORY).isObject()) {
                        JsonNode history = accountDetails.get(ExperianConstants.ACCT_HISTORY);
                        if (!history.get(ExperianConstants.DPD).isNull() && !history.get(ExperianConstants.DPD).asText().equalsIgnoreCase("")) {
                            if (history.get(ExperianConstants.DPD).asInt() >= maxDpd) {
                                count++;
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxNonCreditCardDPD", e);
        }
        return dpd;
    }

    @Override
    public int totalEnquiryLastXMonth(int months) {
        int count = 0;
        try {
            Date reportDate = getReportDate();
            Calendar c = Calendar.getInstance();
            c.setTime(reportDate);
            c.add(Calendar.MONTH, -months);
            String month = (c.get(Calendar.MONTH) + 1) < 10 ? "0" + (c.get(Calendar.MONTH) + 1) : (c.get(Calendar.MONTH) + 1) + "";
            String day = (c.get(Calendar.DAY_OF_MONTH) + 1) < 10 ? "0" + (c.get(Calendar.DAY_OF_MONTH) + 1) : (c.get(Calendar.DAY_OF_MONTH) + 1) + "";
            long previousXMonthDate = Long.parseLong(c.get(Calendar.YEAR) + month + day);
            if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isObject()) {
                JsonNode jsonNode = response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS);
                if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null && jsonNode.get(ExperianConstants.DOR) != null && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                    count++;
                }
            } else if (response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS) != null && response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS).isArray()) {
                for (JsonNode jsonNode : response.get(ExperianConstants.PROFILE_RESPONSE).get("CAPS").get(ExperianConstants.CAPS_DETAILS)) {
                    if (jsonNode.get(ExperianConstants.ENQUIRY_REASON) != null && jsonNode.get(ExperianConstants.DOR) != null && jsonNode.get(ExperianConstants.DOR).longValue() >= previousXMonthDate) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiries in experian", e);
        }
        return count;
    }


    @Override
    public int activeGoldLoan(Double amount) {
        int activeGoldLoanCount = 0;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() == 7 && activeStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        if (accounts.has(ExperianConstants.LOAN_AMOUNT) && !accounts.get(ExperianConstants.LOAN_AMOUNT).asText().equalsIgnoreCase("") && accounts.get(ExperianConstants.LOAN_AMOUNT).doubleValue() < amount) {
                            activeGoldLoanCount++;
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 7 && activeStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    if (accountDetails.has(ExperianConstants.LOAN_AMOUNT) && !accountDetails.get(ExperianConstants.LOAN_AMOUNT).asText().equalsIgnoreCase("") && accountDetails.get(ExperianConstants.LOAN_AMOUNT).doubleValue() < amount) {
                        activeGoldLoanCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking Active Gold Loan Count e:{}", e);
        }

        return activeGoldLoanCount;
    }

    @Override
    public int activePersonalLoan(Double amount) {
        int activePersonalLoanCount = 0;
        try {
            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
            if (accountDetails != null && accountDetails.isArray()) {
                for (JsonNode accounts : accountDetails) {
                    if (accounts.get(ExperianConstants.ACCT_TYPE).asInt() == 5 && activeStatusList.contains(accounts.get(ExperianConstants.ACCT_STATUS).asInt())) {
                        if (amount == 0D) {
                            activePersonalLoanCount++;
                        } else if (accountDetails.has(ExperianConstants.LOAN_AMOUNT) && !accountDetails.get(ExperianConstants.LOAN_AMOUNT).asText().equalsIgnoreCase("") && accountDetails.get(ExperianConstants.LOAN_AMOUNT).doubleValue() < amount) {
                            activePersonalLoanCount++;
                        }
                    }
                }
            } else if (accountDetails != null && accountDetails.isObject()) {
                if (accountDetails.get(ExperianConstants.ACCT_TYPE).asInt() == 5 && activeStatusList.contains(accountDetails.get(ExperianConstants.ACCT_STATUS).asInt())) {
                    if (amount == 0D) {
                        activePersonalLoanCount++;
                    } else if (accountDetails.has(ExperianConstants.LOAN_AMOUNT) && !accountDetails.get(ExperianConstants.LOAN_AMOUNT).asText().equalsIgnoreCase("") && accountDetails.get(ExperianConstants.LOAN_AMOUNT).doubleValue() < amount) {
                        activePersonalLoanCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while checking Personal Loan Count e:{}", e);
        }

        return activePersonalLoanCount;
    }

    @Override
    public Boolean getETC() {
        try {
            if (getBureauScore() >= 300) {
                return true;
            }
            return false;
//            JsonNode accountDetails = response.get(ExperianConstants.PROFILE_RESPONSE).get(ExperianConstants.ACCT).get(ExperianConstants.ACCT_DETAILS);
//            if (accountDetails != null && accountDetails.isArray()) {
//                for (JsonNode jsonNode : accountDetails) {
//                    if (getLoanAmount(jsonNode) >= 1000 && getBureauScore() >= 300) {//loan amount or credit limit > 10k
//                        return true;
//                    }
//                }
//            } else if (accountDetails != null && accountDetails.isObject()) {
//                if (getLoanAmount(accountDetails) >= 1000 && getBureauScore() >= 300) {//loan amount or credit limit > 10k
//                    return true;
//                }
//            }
        } catch (Exception e) {
            logger.info("Exception in getETC in experian", e);
        }
        return false;
    }
}
