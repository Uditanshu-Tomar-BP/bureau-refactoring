package com.bharatpe.lending.bureaurefactoring.utils;

import com.bharatpe.lending.bureaurefactoring.constants.Constant;
import com.bharatpe.lending.bureaurefactoring.constants.CrifConstants;
import com.bharatpe.lending.bureaurefactoring.constants.ExperianConstants;
import com.bharatpe.lending.bureaurefactoring.dto.CreditScoreReportDetailDTO;
import com.bharatpe.lending.bureaurefactoring.dto.LoanAndCreditCardDetailDTO;
import com.bharatpe.lending.bureaurefactoring.dto.NfiCalculationDetailsDto;
import com.bharatpe.lending.bureaurefactoring.enums.Bureau;
import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;
import com.bharatpe.lending.bureaurefactoring.enums.Gender;
import com.bharatpe.lending.bureaurefactoring.nfi.CrifNfiCalculator;
import com.bharatpe.lending.bureaurefactoring.nfi.NfiCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CrifResponseUtil extends ResponseUtilBase implements BureauResponseUtil {
    NfiCalculator crifNfiCalculator = new CrifNfiCalculator();
    List<String> delinquentDPDStatus = Arrays.asList("SUB", "DBT", "SMA", "LOS", "B", "D", "M", "L");
    List<String> unsecuredProducts = CrifConstants.UNSECURED_PRODUCTS;
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
    List<String> unsecuredLoanTypes = CrifConstants.UNSECURED_ACCT_TYPES;
    List<String> unsecuredLoanUnderGstOffer = CrifConstants.UNSECURED_LOAN_FOR_POS;
    List<String> categoryA = Arrays.asList("consumer loan", "gold loan", "two-wheeler loan",
            "prime minister jaan dhan yojana - overdraft", "mudra loans – shishu / kishor / tarun",
            "microfinance others");
    List<String> categoryB = Arrays.asList("auto loan (personal)", "personal loan", "education loan",
            "loan to professional", "credit card", "leasing", "overdraft", "commercial vehicle loan", "used car loan",
            "construction equipment loan", "tractor loan", "kisan credit card", "loan on credit card",
            "business loan general", "business loan priority sector small business",
            "business loan priority sector agriculture", "business loan priority sector others",
            "business non-funded credit facility general",
            "business non-funded credit facility-priority sector- small business",
            "business non-funded credit facility-priority sector-agriculture",
            "business non-funded credit facility-priority sector-others", "business loan against bank deposits",
            "staff loan", "business loan unsecured");
    List<String> categoryC = Arrays.asList("housing loan", "property loan");

    SimpleDateFormat dateFormat = new SimpleDateFormat(CrifConstants.DATE_FORMAT);

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    DateTimeFormatter formatter = DateTimeFormat.forPattern(CrifConstants.DATE_FORMAT);

    public CrifResponseUtil(JsonNode response) {
        this.type = Bureau.CRIF.name();
        this.response = response;
    }

    @Override
    public String getEmail() {
        if (Objects.isNull(response) || Objects.isNull(response.get(CrifConstants.REPORT_HEADER))) {
            return null;
        }
        JsonNode email = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.REQUEST);
        email = email != null ? email.get("EMAIL-1") : null;
        return email != null ? email.asText() : null;
    }

    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public Double getBureauScore() {
        JsonNode bureauScore = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.SCORES);
        bureauScore = bureauScore != null ? bureauScore.get(CrifConstants.SCORE) : null;
        return bureauScore != null && bureauScore.get("SCORE-VALUE") != null ? bureauScore.get("SCORE-VALUE").asDouble()
                : null;
    }

    @Override
    public Date getReportDate() {
        try {
            JsonNode dateOfIssue = response.get(CrifConstants.REPORT_HEADER).get("HEADER");
            dateOfIssue = dateOfIssue != null ? dateOfIssue.get("DATE-OF-ISSUE") : null;
            return dateOfIssue != null ? dateFormat.parse(dateOfIssue.asText()) : null;
        } catch (Exception e) {
            logger.info("Exception in parsing reportDate in CrifResponseUtil", e);
            return null;
        }
    }

    @Override
    public JsonNode getResponse() {
        return this.response;
    }

    @Override
    public int fetchBureauVintage() {
        if(Objects.isNull(response) || Objects.isNull(response.get(CrifConstants.REPORT_HEADER))) {
            return 0;
        }
        DateTime min = new DateTime();
        JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES);
        loanDetails = loanDetails != null ? loanDetails.get(CrifConstants.RESPONSE) : null;
        if (loanDetails != null && loanDetails.isArray()) {
            for (JsonNode jsonNode : loanDetails) {
                try {
                    String openDate = jsonNode.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_DT).asText();
                    min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
                } catch (Exception e) {
                    logger.info("Invalid Open_Date");
                }
            }
            return Days.daysBetween(min, DateTime.now()).getDays();
        } else if (loanDetails != null && loanDetails.isObject()) {
            try {
                String openDate = loanDetails.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_DT).asText();
                min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
            } catch (Exception e) {
                logger.info("Invalid Open_Date");
            }
            return Days.daysBetween(min, DateTime.now()).getDays();
        }
        return 0;
    }

    @Override
    public boolean isValid(String panCard, String phoneNumber) {
        if (StringUtils.isEmpty(panCard)) return true;
        boolean checkPan = false;
        boolean checkPhone = false;
        if (this.response != null) {
            JsonNode personalData = this.response.get(CrifConstants.REPORT_HEADER)
                    .get(CrifConstants.PERSONAL_VARIATIONS);
            if (personalData == null || personalData.toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            if (personalData.get(CrifConstants.PAN_VARIATIONS) == null
                    || personalData.get(CrifConstants.PAN_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            if (personalData.get(CrifConstants.PHONE_VARIATIONS) == null
                    || personalData.get(CrifConstants.PHONE_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                return false;
            }
            List<JsonNode> panVariations = CommonUtil.jsonNodeArrayUtil(personalData.get(CrifConstants.PAN_VARIATIONS).get(CrifConstants.VARIATION));
            List<JsonNode> phoneVariations = CommonUtil.jsonNodeArrayUtil(personalData.get(CrifConstants.PHONE_VARIATIONS).get(CrifConstants.VARIATION));
            if (phoneNumber.length() > 10) {
                phoneNumber = phoneNumber.substring(2);// remove 91
            }
            for (JsonNode pan : panVariations) {
                checkPan = pan.get("VALUE").asText().equalsIgnoreCase(panCard);
                if(checkPan) break;
            }
            for (JsonNode phone : phoneVariations) {
                checkPhone = phone.get("VALUE").asText().equalsIgnoreCase(phoneNumber) || phone.get("VALUE").asText().equalsIgnoreCase("91" + phoneNumber);
                if(checkPhone) break;
            }
        }
        return checkPan && checkPhone;
    }

    public boolean derogChecks(JsonNode jsonNode, Date reportDate) {
        // Check for Derog Account Status
        if (jsonNode.get(CrifConstants.ACCT_STATUS) != null
                && jsonNode.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"WRITTEN-OFF\"")) {
            logger.info("Derog Account Status check failed, rejecting user");
            rejectReason = ExperianConstants.DEROG_ACCOUNT_STATUS;
            return true;
        }
        // Check for Derog DPD Last 3 months
        if (checkDPDLastXmonths(jsonNode, 3, reportDate)) {
            logger.info("Derog DPD Last 3 months check failed, rejecting user");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_3_MONTHS;
            return true;
        }
        // Check for Derog DPD Last 6 months
        if (checkDPDLastXmonths(jsonNode, 6, reportDate)) {
            logger.info("Derog DPD Last 6 months check failed, rejecting user");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_6_MONTHS;
            return true;
        }
        // Check for Derog DPD Last 12 months
        if (checkDPDLastXmonths(jsonNode, 12, reportDate)) {
            logger.info("Derog DPD Last 12 months check failed, rejecting user");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_12_MONTHS;
            return true;
        }
        // Check for Derog DPD Last 24 months
        if (checkDPDLastXmonths(jsonNode, 24, reportDate)) {
            logger.info("Derog DPD Last 24 months check failed, rejecting user");
            rejectReason = ExperianConstants.DEROG_DPD_LAST_24_MONTHS;
            return true;
        }
        return false;
    }

    private boolean checkDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        Date dateReported = null;
        try {
            JsonNode dateRep = jsonNode.get(CrifConstants.DATE_REPORTED);
            if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                dateReported = dateFormat.parse(dateRep.asText());
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
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");
        for (int i = 0; i < months; i++) {
            month = monthFormat.format(c.getTime());
            monthYear.add(month + ":" + c.get(Calendar.YEAR));// Jan:2020
            c.add(Calendar.MONTH, -1);
        }
        int dpdToCheck = 5;// 3 months
        switch (months) {
            case 6:
                dpdToCheck = 30;
                break;
            case 12:
                dpdToCheck = 60;
                break;
            case 24:
                dpdToCheck = 90;
                break;
            default:
                break;
        }
        JsonNode paymentHistory = jsonNode.get("COMBINED-PAYMENT-HISTORY");
        int dpdCount;
        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
            String[] loanHistory = paymentHistory.asText().split("\\|");
            for (String monthNode : loanHistory) {
                String date = monthNode.split(",")[0];
                String code1 = monthNode.split(",")[1].split("/")[0];
                String code2 = monthNode.split(",")[1].split("/")[1];
                if (monthYear.contains(date)) {
                    dpdCount = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                    if (dpdCount >= dpdToCheck)
                        return true;
                }
            }
        }
        return false;
    }

    private boolean checkUnsecuredLiveLoans(JsonNode jsonNode) {
        return (jsonNode.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"ACTIVE\"")
                || jsonNode.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"CURRENT\""))
                && unsecuredProducts.contains(jsonNode.get(CrifConstants.ACCT_TYPE).asText().toLowerCase());
    }

    @Override
    public int countLoanEnquiriesInLast3Months() {
        Date reportDate = getReportDate();
        int inquiryCount = 0;
        try {
            JsonNode inquiryDate;
            JsonNode inquiryHistory = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.INQUIRY_HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return 0;
            inquiryHistory = inquiryHistory.get(CrifConstants.HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return 0;
            if (inquiryHistory.isArray()) {
                for (JsonNode history : inquiryHistory) {
                    inquiryDate = history.get(CrifConstants.INQUIRY_DATE);
                    if (inquiryDate == null)
                        continue;
                    Date inqDate = dateFormat.parse(inquiryDate.asText());
                    if (CommonUtil.getDateDiffInDays(inqDate, reportDate) <= 90)
                        inquiryCount += 1;
                }
            } else if (inquiryHistory.isObject()) {
                inquiryDate = inquiryHistory.get(CrifConstants.INQUIRY_DATE);
                if (inquiryDate == null)
                    return inquiryCount;
                Date inqDate = dateFormat.parse(inquiryDate.asText());
                if (CommonUtil.getDateDiffInDays(inqDate, reportDate) <= 90)
                    inquiryCount += 1;
            }
        } catch (Exception e) {
            logger.error("Exception while checking loan enquiries");
        }
        return inquiryCount;
    }

    @Override
    public int countUnsecuredLoanEnquiriesInLast6Months() throws ParseException {
        Date reportDate = getReportDate();
        int inquiryCount = 0;
        JsonNode inquiryDate;
        JsonNode purpose;
        JsonNode inquiryHistory = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.INQUIRY_HISTORY);
        if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
            return 0;
        inquiryHistory = inquiryHistory.get(CrifConstants.HISTORY);
        if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
            return 0;
        if (inquiryHistory.isArray()) {
            for (JsonNode history : inquiryHistory) {
                inquiryDate = history.get(CrifConstants.INQUIRY_DATE);
                purpose = history.get("PURPOSE");
                if (inquiryDate == null || purpose == null)
                    continue;
                Date inqDate = dateFormat.parse(inquiryDate.asText());
                if (CommonUtil.getDateDiffInDays(inqDate, reportDate) <= 180
                        && unsecuredProducts.contains(purpose.asText().toLowerCase()))
                    inquiryCount += 1;
            }
        } else if (inquiryHistory.isObject()) {
            inquiryDate = inquiryHistory.get(CrifConstants.INQUIRY_DATE);
            purpose = inquiryHistory.get("PURPOSE");
            if (inquiryDate == null || purpose == null)
                return inquiryCount;
            Date inqDate = dateFormat.parse(inquiryDate.asText());
            if (CommonUtil.getDateDiffInDays(inqDate, reportDate) <= 180
                    && unsecuredProducts.contains(purpose.asText().toLowerCase()))
                inquiryCount += 1;
        }
        return inquiryCount;
    }

    private boolean isLoanClosed(JsonNode loan) {
        JsonNode closedDate = loan.has(CrifConstants.CLOSED_DATE) && loan.get(CrifConstants.CLOSED_DATE) != null ? loan.get(CrifConstants.CLOSED_DATE) : null;
        Date clsDate = null;
        try {
            clsDate = closedDate != null ?dateFormat.parse(closedDate.asText()) : null;
        } catch (Exception e) {
            logger.info("Exception while parsing date closed");
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

    private boolean isLoanClosedWithinOneYear(JsonNode loan) {
        String date = (loan.has(CrifConstants.CLOSED_DATE) && !loan.get(CrifConstants.CLOSED_DATE).isNull()
                && !loan.get(CrifConstants.CLOSED_DATE).asText().equalsIgnoreCase(""))
                ? loan.get(CrifConstants.CLOSED_DATE).asText()
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

    private String getLoanType(String loanType) {
        if (loanType == null || loanType.isEmpty()) {
            return "Other";
        }
        loanType = loanType.toLowerCase();
        if (loanTypeAL.contains(loanType)) {
            return "AL";
        } else if (loanTypePL.contains(loanType)) {
            return "PL";
        } else if (loanTypeHL.contains(loanType)) {
            return "HL";
        } else if (loanTypeBL.contains(loanType)) {
            return "BL";
        } else if (loanTypeCC.contains(loanType)) {
            return "CC";
        } else if (loanTypeTW.contains(loanType)) {
            return "TW";
        } else if (loanTypeCD.contains(loanType)) {
            return "CD";
        } else if (loanTypeGL.contains(loanType)) {
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
        if (loan == null || loan.get(CrifConstants.ACCT_TYPE) == null
                || loan.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("\"\"")) {
            debtAndIncome.put("debt", debt);
            debtAndIncome.put("income", income);
            debtAndIncome.put("debtActiveLoans", debtActiveLoans);
            return debtAndIncome;
        }
        double loanAmount = getLoanAmount(loan);
        boolean isLoanClosed = isLoanClosed(loan);
        boolean isLoanClosedWithinAYear = isLoanClosedWithinOneYear(loan);
        String loanType = getLoanType(loan.get(CrifConstants.ACCT_TYPE).asText());
        if (loanAmount >= 5000 && (!isLoanClosed || isLoanClosedWithinAYear)) {
            if (!isLoanClosedWithinAYear) {
                debt += loanAmount * Constant.EMI.get(loanType);
            }
            income += loanAmount * Constant.EMI.get(loanType) / Constant.DBI.get(loanType);
            if (income < Constant.OTHER_INCOME.getOrDefault(loanType, 0D)) {
                income = Constant.OTHER_INCOME.getOrDefault(loanType, 0D);
            }
            if(!isLoanClosed) {
                debtActiveLoans += loanAmount * ExperianConstants.DEBT_EMI.getOrDefault(loanType,0D) / 100000;
            }
            logger.info(
                    "loanStatus: {}, loanAmount:{}, isLoanClosed: {}, isLoanClosedWithinAYear: {}, loanType: {}, income:{}, debt:{}, debtActiveLoans: {}",
                    loan.get(CrifConstants.ACCT_STATUS), loanAmount, isLoanClosed, isLoanClosedWithinAYear, loanType,
                    income, debt, debtActiveLoans);
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
            loan = loan.get(CrifConstants.LOAN_DETAILS);
            if (loan == null || loan.get(CrifConstants.ACCT_TYPE) == null
                    || loan.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("\"\"")) {
                continue;
            }
            double loanAmount = getLoanAmount(loan);
            boolean isLoanClosed = isLoanClosed(loan);
            boolean isLoanClosedWithinAYear = isLoanClosedWithinOneYear(loan);
            String loanType = getLoanType(loan.get(CrifConstants.ACCT_TYPE).asText());
            if (loanAmount >= 5000 && (!isLoanClosed || isLoanClosedWithinAYear)) {
                double income = loanAmount * Constant.EMI.get(loanType) / Constant.DBI.get(loanType);
                incomeMap.put(loanType, incomeMap.getOrDefault(loanType, 0D) + income);
                if (!isLoanClosedWithinAYear) {
                    debt += loanAmount * Constant.EMI.get(loanType);
                }
                if(!isLoanClosed) {
                    debtActiveLoans += loanAmount * ExperianConstants.DEBT_EMI.getOrDefault(loanType,0D) / 100000;
                }
                logger.info(
                        "loanStatus: {}, loanAmount:{}, isLoanClosed: {}, isLoanClosedWithinAYear: {}, loanType: {}, income:{}, debt:{}, debtActiveLoans: {}",
                        loan.get(CrifConstants.ACCT_STATUS), loanAmount, isLoanClosed, isLoanClosedWithinAYear, loanType,
                        income, debt, debtActiveLoans);
            }
        }
        double totalIncome = 0d;
        for (String loanType : incomeMap.keySet()) {
            totalIncome += Math.max(incomeMap.get(loanType), Constant.OTHER_INCOME.getOrDefault(loanType, 0D));
        }
        debtAndIncome.put("debt", debt);
        debtAndIncome.put("income", totalIncome);
        debtAndIncome.put("debtActiveLoans", debtActiveLoans);
        return debtAndIncome;
    }

    private int countDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        Date dateReported = null;
        try {
            JsonNode dateRep = jsonNode.get(CrifConstants.DATE_REPORTED);
            if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                dateReported = dateFormat.parse(dateRep.asText());
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
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");
        for (int i = 0; i < months; i++) {
            month = monthFormat.format(c.getTime());
            monthYear.add(month + ":" + c.get(Calendar.YEAR));// Jan:2020
            c.add(Calendar.MONTH, -1);
        }
        JsonNode paymentHistory = jsonNode.get("COMBINED-PAYMENT-HISTORY");
        int dpdCount = 0;
        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
            String[] loanHistory = paymentHistory.asText().split("\\|");
            for (String monthNode : loanHistory) {
                String date = monthNode.split(",")[0];
                String code1 = monthNode.split(",")[1].split("/")[0];
                String code2 = monthNode.split(",")[1].split("/")[1];
                if (monthYear.contains(date)) {
                    int loanDpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                    if (loanDpd > 5) {
                        dpdCount++;
                    }
                }
            }
        }
        return dpdCount;
    }

    private int codeToCount(String code) {
        try {
            return Integer.parseInt(code);
        } catch (Exception e) {
            logger.info("Bad DPD Count String Exception");
            return 0;
        }
    }

    private int loanSanctioned3mon(JsonNode jsonNode, Date reportDate) throws ParseException {
        if (jsonNode.get(CrifConstants.DISBURSED_DT) != null
                && !jsonNode.get(CrifConstants.DISBURSED_DT).toString().equalsIgnoreCase("\"\"")) {
            Date openDate = dateFormat.parse(jsonNode.get(CrifConstants.DISBURSED_DT).asText());
            return CommonUtil.getDateDiffInDays(openDate, reportDate) <= 90 ? 1 : 0;
        }
        return 0;
    }

    private int unsecuredLoan6mon(JsonNode jsonNode, Date reportDate) throws ParseException {
        JsonNode acctType = jsonNode.get(CrifConstants.ACCT_TYPE);
        JsonNode openDt = jsonNode.get(CrifConstants.DISBURSED_DT);
        if (acctType != null && openDt != null && !openDt.toString().equalsIgnoreCase("\"\"")) {
            Date openDate = dateFormat.parse(openDt.asText());
            return CommonUtil.getDateDiffInDays(openDate, reportDate) <= 180
                    && unsecuredLoanTypes.contains(acctType.asText().toLowerCase()) ? 1 : 0;
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
        Set<String> loanTypes = new HashSet<>();
        JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)
                .get(CrifConstants.RESPONSE);
        if (loanDetails != null && loanDetails.isObject()) {
            loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
            JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
            JsonNode openDt = loanDetails.get(CrifConstants.DISBURSED_DT);
            nfiCalculationDetailsDto = crifNfiCalculator.getDebtAndIncomeDetails(loanDetails);
            activeUslPos = getActiveUslPos(loanDetails);
            delinquencyCount6mon += countDPDLastXmonths(loanDetails, 6, reportDate);
            loanSanctioned3mon += loanSanctioned3mon(loanDetails, reportDate);
            unsecuredLoanCount6mon += unsecuredLoan6mon(loanDetails, reportDate);
            loanTypes.add(getLoanType(acctType.asText()));
            if (openDt != null && !openDt.toString().equalsIgnoreCase("\"\"")) {
                Date openDate = dateFormat.parse(openDt.asText());
                if (openDate.before(minOpenDate)) {
                    minOpenDate = openDate;
                }
            }
        } else if (loanDetails != null && loanDetails.isArray()) {
            nfiCalculationDetailsDto = crifNfiCalculator.getDebtAndIncomeDetails((ArrayNode) loanDetails);
            activeUslPos = getActiveUslPos(loanDetails);
            for (JsonNode detail : loanDetails) {
                detail = detail.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                JsonNode openDt = detail.get(CrifConstants.DISBURSED_DT);
                delinquencyCount6mon += countDPDLastXmonths(detail, 6, reportDate);
                loanSanctioned3mon += loanSanctioned3mon(detail, reportDate);
                unsecuredLoanCount6mon += unsecuredLoan6mon(detail, reportDate);
                loanTypes.add(getLoanType(acctType.asText()));
                if (openDt != null && !openDt.toString().equalsIgnoreCase("\"\"")) {
                    Date openDate = dateFormat.parse(openDt.asText());
                    if (openDate.before(minOpenDate)) {
                        minOpenDate = openDate;
                    }
                }
            }
        }
        res.put("activeUnsecuredLoanAmount",activeUslPos);
        res.put("debtAndIncome", nfiCalculationDetailsDto);
        res.put("delinquencyCount6mon", delinquencyCount6mon);
        res.put("loanSanctioned3mon", loanSanctioned3mon);
        res.put("unsecuredLoanCount6mon", unsecuredLoanCount6mon);
        res.put("minOpenDate", minOpenDate);
        res.put("loanTypes", loanTypes);
        return res;
    }

    private Double getActiveUslPos(JsonNode loanDetails) {
        Double activeUslPos = 0D;
        try {
            if (!loanDetails.isArray()) {
                logger.info("getActiveUslPos -> loan: {}", loanDetails);
                String loanCode = loanDetails.get(CrifConstants.ACCT_TYPE).asText();
                if (!isLoanClosed(loanDetails) && unsecuredLoanUnderGstOffer.contains(loanCode.toLowerCase())) {
                    return Math.abs(getCurrentBalance(loanDetails));
                }
                return activeUslPos;
            }
            for (JsonNode loan : loanDetails) {
                loan = loan.get(CrifConstants.LOAN_DETAILS);
                logger.info("getActiveUslPos -> loan: {}", loan);
                String loanCode = loan.get(CrifConstants.ACCT_TYPE).asText();
                if (!isLoanClosed(loan) && unsecuredLoanUnderGstOffer.contains(loanCode.toLowerCase())) {
                    activeUslPos = ObjectUtils.isEmpty(activeUslPos) ? 0:activeUslPos;
                    activeUslPos+= Math.abs(getCurrentBalance(loan));
                }
            }
            return activeUslPos;
        } catch (Exception e) {
            logger.error("Exception occurred while fetching active Usl amount",e);
        }
        return null;
    }

    @Override
    public int getLoanCount(BureauLoanType loanType) {
        int count = 0;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return count;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        count++;
                    } else if (getLoanAmount(loanDetails) >= 10000 && !isLoanClosed(loanDetails)) {//credit card with amount>=10k and active
                        count++;
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            count++;
                        } else if (getLoanAmount(detail) >= 10000 && !isLoanClosed(detail)) {//credit card with amount>=10k and active
                            count++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in getLoanCount in crif", e);
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
            logger.info("Exception while getting age from crif", e);
        }
        return age;
    }

    @Override
    public Boolean getLoanSettlement() {
        if(Objects.nonNull(response)) {
            return true;
        }
        try {
            if (response.get(CrifConstants.ACCT_STATUS) != null
                    && response.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"WRITTEN-OFF\"")) {
                logger.info("Derog Account Status check failed, rejecting user");
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
            JsonNode personalData = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.PERSONAL_VARIATIONS);
            if (personalData != null && personalData.get(CrifConstants.DOB_VARIATIONS) != null && !personalData.get(CrifConstants.DOB_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                List<JsonNode> dobVariations = CommonUtil.jsonNodeArrayUtil(personalData.get(CrifConstants.DOB_VARIATIONS).get(CrifConstants.VARIATION));
                for (JsonNode dobVariation : dobVariations) {
                    String value = dobVariation.get("VALUE").asText();
                    Date dateOfBirth = dateFormat.parse(value);
                    dob = simpleDateFormat.format(dateOfBirth);
                    break;
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getDOB from crif", e);
        }
        return dob;
    }

    @Override
    public int getMaxDPD(int months) {
        int dpd = 0;
        try {
            Date reportDate = getReportDate();
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                dpd = maxDPDLastXmonths(loanDetails, months, reportDate);
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    dpd = Math.max(dpd, maxDPDLastXmonths(detail, months, reportDate));
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getMaxDPD from crif", e);
        }
        return dpd;
    }

    @Override
    public boolean writtenOffLast12Months() {
        boolean writtenOff = false;
        try {
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                if (loanDetails.get(CrifConstants.ACCT_STATUS) != null && loanDetails.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"WRITTEN-OFF\"")) {
                    writtenOff = true;
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    if (detail.get(CrifConstants.ACCT_STATUS) != null && detail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"WRITTEN-OFF\"")) {
                        writtenOff = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in checking written off in crif", e);
        }
        return writtenOff;
    }

    @Override
    public int getMaxOverdueAmount() {
        return 0;//TODO
    }

    @Override
    public int countUnsecuredLoanEnquiries(int months) {
        int inquiryCount = 0;
        try {
            Date reportDate = getReportDate();
            JsonNode inquiryDate;
            JsonNode purpose;
            JsonNode inquiryHistory = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.INQUIRY_HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            inquiryHistory = inquiryHistory.get(CrifConstants.HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            if (inquiryHistory.isArray()) {
                for (JsonNode history : inquiryHistory) {
                    inquiryDate = history.get(CrifConstants.INQUIRY_DATE);
                    purpose = history.get("PURPOSE");
                    if (inquiryDate == null || purpose == null)
                        continue;
                    Date inqDate = dateFormat.parse(inquiryDate.asText());
                    if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= months && unsecuredProducts.contains(purpose.asText().toLowerCase()))
                        inquiryCount += 1;
                }
            } else if (inquiryHistory.isObject()) {
                inquiryDate = inquiryHistory.get(CrifConstants.INQUIRY_DATE);
                purpose = inquiryHistory.get("PURPOSE");
                if (inquiryDate == null || purpose == null)
                    return inquiryCount;
                Date inqDate = dateFormat.parse(inquiryDate.asText());
                if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= months
                        && unsecuredProducts.contains(purpose.asText().toLowerCase()))
                    inquiryCount += 1;
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiriesInLast3Months in crif", e);
        }
        return inquiryCount;
    }

    @Override
    public int countSecuredLoanEnquiries(int months) {
        int inquiryCount = 0;
        try {
            Date reportDate = getReportDate();
            JsonNode inquiryDate;
            JsonNode purpose;
            JsonNode inquiryHistory = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.INQUIRY_HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            inquiryHistory = inquiryHistory.get(CrifConstants.HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            if (inquiryHistory.isArray()) {
                for (JsonNode history : inquiryHistory) {
                    inquiryDate = history.get(CrifConstants.INQUIRY_DATE);
                    purpose = history.get("PURPOSE");
                    if (inquiryDate == null || purpose == null)
                        continue;
                    Date inqDate = dateFormat.parse(inquiryDate.asText());
                    if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= months && !unsecuredProducts.contains(purpose.asText().toLowerCase()))
                        inquiryCount += 1;
                }
            } else if (inquiryHistory.isObject()) {
                inquiryDate = inquiryHistory.get(CrifConstants.INQUIRY_DATE);
                purpose = inquiryHistory.get("PURPOSE");
                if (inquiryDate == null || purpose == null)
                    return inquiryCount;
                Date inqDate = dateFormat.parse(inquiryDate.asText());
                if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= months
                        && !unsecuredProducts.contains(purpose.asText().toLowerCase()))
                    inquiryCount += 1;
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiriesInLast3Months in crif", e);
        }
        return inquiryCount;
    }

    @Override
    public double maxCurrentBalance(BureauLoanType loanType) {
        double max = 0D;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return max;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        max = Math.max(max, getCurrentBalance(loanDetails));
                    } else if (getCurrentBalance(loanDetails) >= 10000) {
                        max = Math.max(max, getCurrentBalance(loanDetails));
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            max = Math.max(max, getCurrentBalance(detail));
                        } else if (getCurrentBalance(detail) >= 10000) {
                            max = Math.max(max, getCurrentBalance(detail));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxLoanAmount in crif", e);
        }
        return max;
    }

    @Override
    public double maxLoanAmount(BureauLoanType loanType) {
        double max = 0D;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return max;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        max = Math.max(max, getLoanAmount(loanDetails));
                    } else if (getLoanAmount(loanDetails) >= 10000) {
                        max = Math.max(max, getLoanAmount(loanDetails));
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            max = Math.max(max, getLoanAmount(detail));
                        } else if (getLoanAmount(detail) >= 10000) {
                            max = Math.max(max, getLoanAmount(detail));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxLoanAmount in crif", e);
        }
        return max;
    }

    @Override
    public double minLoanAmount(BureauLoanType loanType) {
        double min = Double.MAX_VALUE;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return 0;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        min = Math.min(min, getLoanAmount(loanDetails));
                    } else if (getLoanAmount(loanDetails) >= 10000) {
                        min = Math.min(min, getLoanAmount(loanDetails));
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            min = Math.min(min, getLoanAmount(detail));
                        } else if (getLoanAmount(detail) >= 10000) {
                            min = Math.min(min, getLoanAmount(detail));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in minLoanAmount in crif", e);
        }
        return min == Double.MAX_VALUE ? 0 : min;
    }

    @Override
    public double totalLoanAmount(BureauLoanType loanType) {
        double total = 0D;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return total;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        total += getLoanAmount(loanDetails);
                    } else if (getLoanAmount(loanDetails) >= 10000) {
                        total += getLoanAmount(loanDetails);
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            total += getLoanAmount(detail);
                        } else if (getLoanAmount(detail) >= 10000) {
                            total += getLoanAmount(detail);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in totalLoanAmount in crif", e);
        }
        return total;
    }

    @Override
    public Integer getVintage(BureauLoanType loanType) {
        Integer vintage = null;
        DateTime min = new DateTime();
        boolean loanFound = false;
        try {
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(loanType);
            if (CollectionUtils.isEmpty(accountTypes)) return vintage;
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (accountTypes.contains(acctType.asText())) {
                    if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                        try {
                            String openDate = loanDetails.get(CrifConstants.DISBURSED_DT).asText();
                            min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
                            loanFound = true;
                        } catch (Exception e) {
                            logger.info("Invalid Open_Date");
                        }
                    } else if (getLoanAmount(loanDetails) >= 10000) {
                        try {
                            String openDate = loanDetails.get(CrifConstants.DISBURSED_DT).asText();
                            min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
                            loanFound = true;
                        } catch (Exception e) {
                            logger.info("Invalid Open_Date");
                        }
                    }
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (accountTypes.contains(acctType.asText())) {
                        if (!loanType.equals(BureauLoanType.CREDIT_CARD)) {
                            try {
                                String openDate = detail.get(CrifConstants.DISBURSED_DT).asText();
                                min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
                                loanFound = true;
                            } catch (Exception e) {
                                logger.info("Invalid Open_Date");
                            }
                        } else if (getLoanAmount(detail) >= 10000) {
                            try {
                                String openDate = detail.get(CrifConstants.DISBURSED_DT).asText();
                                min = formatter.parseDateTime(openDate).isBefore(min) ? formatter.parseDateTime(openDate) : min;
                                loanFound = true;
                            } catch (Exception e) {
                                logger.info("Invalid Open_Date");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.info("Exception in getVintage in crif", e);
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
            List<String> accountTypes = CrifConstants.LOAN_TYPE_ACC.get(BureauLoanType.CREDIT_CARD);
            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (loanDetails != null && loanDetails.isObject()) {
                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
                JsonNode acctType = loanDetails.get(CrifConstants.ACCT_TYPE);
                if (!accountTypes.contains(acctType.asText())) {//not a credit card
                    count++;
                } else if (getLoanAmount(loanDetails) >= 10000 && !isLoanClosed(loanDetails)) {//credit card with amount>=10k and active
                    count++;
                }
            } else if (loanDetails != null && loanDetails.isArray()) {
                for (JsonNode detail : loanDetails) {
                    detail = detail.get(CrifConstants.LOAN_DETAILS);
                    JsonNode acctType = detail.get(CrifConstants.ACCT_TYPE);
                    if (!accountTypes.contains(acctType.asText())) {//not a credit card
                        count++;
                    } else if (getLoanAmount(detail) >= 10000 && !isLoanClosed(detail)) {//credit card with amount>=10k and active
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in getTotalLoanCount in crif", e);
        }
        return count;
    }

    @Override
    public String getPancard() {
        try {
            if (this.response != null) {
                JsonNode personalData = this.response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.PERSONAL_VARIATIONS);
                if (personalData == null || personalData.toString().equalsIgnoreCase("\"\"")) {
                    return null;
                }
                if (personalData.get(CrifConstants.PAN_VARIATIONS) == null || personalData.get(CrifConstants.PAN_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                    return null;
                }
                List<JsonNode> panVariations = CommonUtil.jsonNodeArrayUtil(personalData.get(CrifConstants.PAN_VARIATIONS).get(CrifConstants.VARIATION));
                for (JsonNode pan : panVariations) {
                    return pan.get("VALUE").asText();
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching pancard from crif", e);
        }
        return null;
    }

    @Override
    public List<String> getAddress() {
        List<String> addressList = null;
        try {
            if (Objects.nonNull(this.response) && Objects.nonNull(this.response.get(CrifConstants.REPORT_HEADER))) {
                JsonNode personalData = this.response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.PERSONAL_VARIATIONS);
                if (personalData == null || personalData.toString().equalsIgnoreCase("\"\"")) {
                    return null;
                }
                if (personalData.get(CrifConstants.ADDRESS_VARIATIONS) == null || personalData.get(CrifConstants.ADDRESS_VARIATIONS).toString().equalsIgnoreCase("\"\"")) {
                    return null;
                }
                List<JsonNode> addressVariations = CommonUtil.jsonNodeArrayUtil(personalData.get(CrifConstants.ADDRESS_VARIATIONS).get(CrifConstants.VARIATION));
                for (JsonNode addressJson : addressVariations) {
                    String address = addressJson.get("VALUE").asText();
                    if (!StringUtils.isEmpty(address)) {
                        if (addressList == null) {
                            addressList = new ArrayList<>();
                        }
                        addressList.add(address);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching address from crif", e);
        }
        return addressList;
    }

    @Override
    public Gender getGender() {//TODO
        return null;
    }

    @Override
    public double unsecuredLoanUtilization() {
        Double currentBalance = 0D;
        Double sanctionAmount = 0D;
        Double utilization=0D;
        try {
            JsonNode unsecureLoan = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (unsecureLoan != null && unsecureLoan.isArray()) {
                for (JsonNode res : unsecureLoan) {
                    JsonNode loanDetail = res.get(CrifConstants.LOAN_DETAILS);
                    if (loanDetail != null && checkUnsecuredLiveLoans(loanDetail)) {
                        if (Objects.nonNull(loanDetail.get(CrifConstants.CREDIT_LIMIT))) {
                            sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get(CrifConstants.CREDIT_LIMIT).asText().replace(",", ""));
                        } else if (Objects.nonNull(loanDetail.get(CrifConstants.DISBURSED_AMT))) {
                            sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get(CrifConstants.DISBURSED_AMT).asText().replace(",", ""));
                        } else {
                            sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get("CURRENT-BAL").asText().replace(",", ""));
                        }
                        if (Objects.nonNull(loanDetail.get("CURRENT-BAL"))) {
                            currentBalance = currentBalance + Double.parseDouble(loanDetail.get("CURRENT-BAL").asText().replace(",", ""));
                        }
                    }
                }
            } else if (unsecureLoan != null && unsecureLoan.isObject()) {
                JsonNode loanDetail = unsecureLoan.get(CrifConstants.LOAN_DETAILS);
                if (loanDetail != null && checkUnsecuredLiveLoans(loanDetail)) {
                    if (Objects.nonNull(loanDetail.get(CrifConstants.CREDIT_LIMIT))) {
                        sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get(CrifConstants.CREDIT_LIMIT).asText().replace(",", ""));
                    } else if (Objects.nonNull(loanDetail.get(CrifConstants.DISBURSED_AMT))) {
                        sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get(CrifConstants.DISBURSED_AMT).asText().replace(",", ""));
                    } else {
                        sanctionAmount = sanctionAmount + Double.parseDouble(loanDetail.get("CURRENT-BAL").asText().replace(",", ""));
                    }
                    if (Objects.nonNull(loanDetail.get("CURRENT-BAL"))) {
                        currentBalance = currentBalance + Double.parseDouble(loanDetail.get("CURRENT-BAL").asText().replace(",", ""));
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
        Double pastDue=0D;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if ((loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"ACTIVE\"") || loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"CURRENT\"")) && !loanDetail.get(CrifConstants.ACCT_TYPE).toString().equals("Credit Card")) {
                        try {
                            JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                            if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                                dateReported = dateFormat.parse(dateRep.asText());
                            }
                        } catch (Exception e) {
                            logger.error("Exception while Parsing Reported Date:", e);
                        }

                        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                            if (Objects.nonNull(account.get(CrifConstants.OVERDUE_AMT)) && account.get(CrifConstants.OVERDUE_AMT).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + account.get(CrifConstants.OVERDUE_AMT).doubleValue();
                            }
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if ((loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"ACTIVE\"") || loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"CURRENT\"")) && !loanDetail.get(CrifConstants.ACCT_TYPE).toString().equals("Credit Card")) {
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while Parsing Reported Date:", e);
                    }

                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (Objects.nonNull(accounts.get(CrifConstants.OVERDUE_AMT)) && accounts.get(CrifConstants.OVERDUE_AMT).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accounts.get(CrifConstants.OVERDUE_AMT).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in nonCreditOverDuePastXMonthAndActive", e);
        }
        return pastDue;
    }

    @Override
    public Double creditCardOverDuePastXMonth(int month) {
        Date dateReported = null;
        Date reportDate = getReportDate();
        Double pastDue=0D;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if ((loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"ACTIVE\"") || loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"CURRENT\"")) && loanDetail.get(CrifConstants.ACCT_TYPE).toString().equals("Credit Card")) {
                        try {
                            JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                            if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                                dateReported = dateFormat.parse(dateRep.asText());
                            }
                        } catch (Exception e) {
                            logger.error("Exception while Parsing Reported Date:", e);
                        }
                        if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                            if (Objects.nonNull(account.get(CrifConstants.OVERDUE_AMT)) && account.get(CrifConstants.OVERDUE_AMT).asText().equalsIgnoreCase("")) {
                                pastDue = pastDue + account.get(CrifConstants.OVERDUE_AMT).doubleValue();
                            }
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if ((loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"ACTIVE\"") || loanDetail.get(CrifConstants.ACCT_STATUS).asText().equalsIgnoreCase("\"CURRENT\"")) && loanDetail.get(CrifConstants.ACCT_TYPE).toString().equals("Credit Card")) {
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while Parsing Reported Date:", e);
                    }

                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (Objects.nonNull(accounts.get(CrifConstants.OVERDUE_AMT)) && accounts.get(CrifConstants.OVERDUE_AMT).asText().equalsIgnoreCase("")) {
                            pastDue = pastDue + accounts.get(CrifConstants.OVERDUE_AMT).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in creditCardOverDuePastXMonth", e);
        }
        return pastDue;
    }

    @Override
    public Double closedLoanWithOverDueLastXMonth(int month) {
        Date dateClosed=null;
        Date reportDate = getReportDate();
        Double pastDue=0D;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if (isLoanClosed(loanDetail)) {
                        try {
                            if (accounts.get(CrifConstants.CLOSED_DATE) != null && !accounts.get(CrifConstants.CLOSED_DATE).asText().equalsIgnoreCase("")) {
                                dateClosed = dateFormat.parse(accounts.get(CrifConstants.CLOSED_DATE).asText());
                            }
                        } catch (Exception e) {
                            logger.error("Exception While Fetching Closed Loan :{}", e);
                        }
                        if (dateClosed != null && CommonUtil.getDateDiffInDays(dateClosed, reportDate) < month * 30) {
                            if (accounts.has(CrifConstants.WRITE_OFF_AMT) && account.get(CrifConstants.WRITE_OFF_AMT) != null) {
                                pastDue = pastDue + account.get(CrifConstants.WRITE_OFF_AMT).doubleValue();
                            } else if (account.has(CrifConstants.OVERDUE_AMT) && account.get(CrifConstants.OVERDUE_AMT) != null) {
                                pastDue = pastDue + account.get(CrifConstants.OVERDUE_AMT).doubleValue();
                            }
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if (isLoanClosed(loanDetail)) {
                    try {
                        if (accounts.get(CrifConstants.CLOSED_DATE) != null && !accounts.get(CrifConstants.CLOSED_DATE).asText().equalsIgnoreCase("")) {
                            dateClosed = dateFormat.parse(accounts.get(CrifConstants.CLOSED_DATE).asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception While Fetching Closed Loan :{}", e);
                    }
                    if (dateClosed != null && CommonUtil.getDateDiffInDays(dateClosed, reportDate) < month * 30) {
                        if (accounts.has(CrifConstants.WRITE_OFF_AMT) && accounts.get(CrifConstants.WRITE_OFF_AMT) != null) {
                            pastDue = pastDue + accounts.get(CrifConstants.WRITE_OFF_AMT).doubleValue();
                        } else if (accounts.has(CrifConstants.OVERDUE_AMT) && accounts.get(CrifConstants.OVERDUE_AMT) != null) {
                            pastDue = pastDue + accounts.get(CrifConstants.OVERDUE_AMT).doubleValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in closedLoanWithOverDueLastXMonth", e);
        }
        return pastDue;
    }

    @Override
    public Double settleLoanPastXMonth(int month) {
        Date dateReported = null;
        Date reportDate = getReportDate();
        Double pastDue=0D;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    try {
                        JsonNode dateRep = account.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while Parsing Reported Date:", e);
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        if (loanDetail.has(CrifConstants.WRITTEN_OFF_SETTLED_STATUS) && (loanDetail.get(CrifConstants.WRITTEN_OFF_SETTLED_STATUS).toString().equalsIgnoreCase("\"SETTLED\"") || loanDetail.get(CrifConstants.WRITTEN_OFF_SETTLED_STATUS).toString().equalsIgnoreCase("\"WRITTEN-OFF\""))) {
                            pastDue = pastDue + account.get(CrifConstants.WRITE_OFF_AMT).doubleValue();
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                try {
                    JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                    if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                        dateReported = dateFormat.parse(dateRep.asText());
                    }
                } catch (Exception e) {
                    logger.error("Exception while Parsing Reported Date:", e);
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                    if (loanDetail.has(CrifConstants.WRITTEN_OFF_SETTLED_STATUS) && (loanDetail.get(CrifConstants.WRITTEN_OFF_SETTLED_STATUS).toString().equalsIgnoreCase("\"SETTLED\"") || loanDetail.get(CrifConstants.WRITTEN_OFF_SETTLED_STATUS).toString().equalsIgnoreCase("\"WRITTEN-OFF\""))) {
                        pastDue = pastDue + accounts.get(CrifConstants.WRITE_OFF_AMT).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in settleLoanPastXMonth", e);
        }
        return pastDue;
    }

    @Override
    public int maxCreditCardTradeMoreThan60(int maxDpd) {
        int dpd=0;
        int count=0;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
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
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                    if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
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
            }
        } catch (Exception e) {
            logger.error("Exception in maxCreditCardDPD", e);
        }
        return dpd;
    }

    @Override
    public int maxNonCreditCardTradeMoreThan60(int maxDpd) {
        int dpd=0;
        int count=0;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if (!loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
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
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if (!loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                    if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("Credit Card")) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
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
            }
        } catch (Exception e) {
            logger.error("Exception in maxNonCreditCardDPD", e);
        }
        return dpd;
    }

    @Override
    public int totalEnquiryLastXMonth(int month) {
        int inquiryCount = 0;
        try {
            Date reportDate = getReportDate();
            JsonNode inquiryDate;
            JsonNode purpose;
            JsonNode inquiryHistory = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.INQUIRY_HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            inquiryHistory = inquiryHistory.get(CrifConstants.HISTORY);
            if (inquiryHistory == null || inquiryHistory.toString().equalsIgnoreCase("\"\""))
                return inquiryCount;
            if (inquiryHistory.isArray()) {
                for (JsonNode history : inquiryHistory) {
                    inquiryDate = history.get(CrifConstants.INQUIRY_DATE);
                    purpose = history.get("PURPOSE");
                    if (inquiryDate == null || purpose == null)
                        continue;
                    Date inqDate = dateFormat.parse(inquiryDate.asText());
                    if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= month)
                        inquiryCount += 1;
                }
            } else if (inquiryHistory.isObject()) {
                inquiryDate = inquiryHistory.get(CrifConstants.INQUIRY_DATE);
                purpose = inquiryHistory.get("PURPOSE");
                if (inquiryDate == null || purpose == null)
                    return inquiryCount;
                Date inqDate = dateFormat.parse(inquiryDate.asText());
                if (CommonUtil.getDateDiffInMonths(inqDate, reportDate) <= month)
                    inquiryCount += 1;
            }
        } catch (Exception e) {
            logger.error("Exception in countUnsecuredLoanEnquiriesInLast3Months in crif", e);
        }
        return inquiryCount;
    }

    @Override
    public int activeGoldLoan(Double amount) {
        int activeGoldLoanCount = 0;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("gold loan") && !isLoanClosed(loanDetail)) {
                        if (getLoanAmount(loanDetail) < amount) {
                            activeGoldLoanCount++;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("gold loan") && !isLoanClosed(loanDetail)) {
                    if (getLoanAmount(loanDetail) < amount) {
                        activeGoldLoanCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in activeGoldLoan", e);
        }
        return activeGoldLoanCount;
    }

    @Override
    public int activePersonalLoan(Double amount) {
        int activePersonalLoanCount = 0;
        try {
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("personal loan") && !isLoanClosed(loanDetail)) {
                        if (amount == 0D) {
                            activePersonalLoanCount++;
                        } else if (getLoanAmount(loanDetail) < amount) {
                            activePersonalLoanCount++;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                if (loanDetail.get(CrifConstants.ACCT_TYPE).toString().equalsIgnoreCase("personal loan") && !isLoanClosed(loanDetail)) {
                    if (amount == 0D) {
                        activePersonalLoanCount++;
                    } else if (getLoanAmount(loanDetail) < amount) {
                        activePersonalLoanCount++;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in activePersonalLoan", e);
        }
        return activePersonalLoanCount;
    }

    private int maxDPDLastXmonths(JsonNode jsonNode, int months, Date reportDate) {
        int max = 0;
        try {
            Date dateReported = null;
            try {
                JsonNode dateRep = jsonNode.get(CrifConstants.DATE_REPORTED);
                if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                    dateReported = dateFormat.parse(dateRep.asText());
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
            SimpleDateFormat monthFormat = new SimpleDateFormat("MMM");
            for (int i = 0; i < months; i++) {
                month = monthFormat.format(c.getTime());
                monthYear.add(month + ":" + c.get(Calendar.YEAR));// Jan:2020
                c.add(Calendar.MONTH, -1);
            }
            JsonNode paymentHistory = jsonNode.get("COMBINED-PAYMENT-HISTORY");
            int dpdCount;
            if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                String[] loanHistory = paymentHistory.asText().split("\\|");
                for (String monthNode : loanHistory) {
                    String date = monthNode.split(",")[0];
                    String code1 = monthNode.split(",")[1].split("/")[0];
                    String code2 = monthNode.split(",")[1].split("/")[1];
                    if (monthYear.contains(date)) {
                        dpdCount = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                        max = Math.max(dpdCount, max);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in maxDPDLastXmonths", e);
        }
        return max;
    }

    public CreditScoreReportDetailDTO getCreditDetailReport(JsonNode bureauResponse){

        CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
        try{
            CreditScoreReportDetailDTO.CreditCardUtilization creditCardUtilization = getCreditCardUtilization(bureauResponse);
            CreditScoreReportDetailDTO.PaymentHistory paymentHistory = getPaymentHistory(bureauResponse);
            CreditScoreReportDetailDTO.AgeOfAccount ageOfAccount = getAgeOfAccount(bureauResponse);
            CreditScoreReportDetailDTO.TotalAccount totalAccount = getTotalAccount(bureauResponse);
            CreditScoreReportDetailDTO.CreditEnquries creditEnquries= getCreditEnquiries(bureauResponse);
            creditScoreReportDetailDTO.setCreditEnquries(creditEnquries);
            creditScoreReportDetailDTO.setCreditCardUtilization(creditCardUtilization);
            creditScoreReportDetailDTO.setAgeOfAccount(ageOfAccount);
            creditScoreReportDetailDTO.setTotalAccount(totalAccount);
            creditScoreReportDetailDTO.setPaymentHistory(paymentHistory);
            creditScoreReportDetailDTO.setExperianNumber(getExperianNumber(bureauResponse));
        }catch(Exception ex){
            logger.error("Error Occurred , Error :{0}", ex);

        }

        return creditScoreReportDetailDTO;
    }

    public CreditScoreReportDetailDTO.CreditCardUtilization getCreditCardUtilization(JsonNode bureauResponse){

        try{
            boolean responseCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE));
            int cardLimit = 0;
            int currentBalance = 0;
            int totalUtilization = 0;
            String impact = null;

            if(responseCheck){
                JsonNode responses = bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);

                if(responses.isObject() && (responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.ACCT_TYPE).asText().equals("Credit Card") || responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.ACCT_TYPE).asText().equals("Corporate Credit Card")) && !isLoanClosed(responses.get(CrifConstants.LOAN_DETAILS))){
                    if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT))){
                        cardLimit += Math.max(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT).asText().replace(",", "")), 0);
                    }else if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                        cardLimit += Math.max(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")), 0);
                    }
                    if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get("CURRENT-BAL"))){
                        currentBalance += Math.max(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get("CURRENT-BAL").asText().replace(",", "")), 0);
                    }
                }else if(responses.isArray()){
                    for(JsonNode response: responses){
                        if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.ACCT_TYPE)) && (response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.ACCT_TYPE).asText().equals("Credit Card") || response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.ACCT_TYPE).asText().equals("Corporate Credit Card") && !isLoanClosed(response.get(CrifConstants.LOAN_DETAILS)))){
                            if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT))){
                                cardLimit += Math.max(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT).asText().replace(",", "")), 0);
                            }else if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                                cardLimit += Math.max(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")), 0);
                            }
                            if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get("CURRENT-BAL"))){
                                currentBalance += Math.max(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get("CURRENT-BAL").asText().replace(",", "")), 0);
                            }
                        }
                    }
                }
            }

            if(cardLimit !=0){
                totalUtilization = (currentBalance * 100)/cardLimit;
            }

            if(totalUtilization < 25){
                impact = "excellent";
            }else if(totalUtilization < 75){
                impact = "average";
            }else {
                impact = "bad";
            }
            CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
            CreditScoreReportDetailDTO.CreditCardUtilization creditCardUtilization = creditScoreReportDetailDTO.new CreditCardUtilization();

            if(totalUtilization == 0 && currentBalance == 0 && cardLimit == 0){
                return null;
            }
            creditCardUtilization.setTotalUtilization(totalUtilization);
            creditCardUtilization.setCardUtilization(currentBalance);
            creditCardUtilization.setCardLimit(cardLimit);
            creditCardUtilization.setImpact(impact);

            return creditCardUtilization;
        }catch(Exception ex){
            logger.error("Error Occurred while calculating card utilization Error :{0}", ex);
        }
        return null;
    }

    public CreditScoreReportDetailDTO.PaymentHistory getPaymentHistory(JsonNode bureauResponse){

        try{
            boolean responseCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE));
            int totalPayment = 0;
            int onTimePayment = 0;
            int deqPayment = 0;
            int timelyPayment = 0;
            String impact = null;

            if(responseCheck) {
                JsonNode responses = bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);

                if (responses.isObject() && Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS))) {
                    JsonNode paymentHistory = responses.get(CrifConstants.LOAN_DETAILS).get("COMBINED-PAYMENT-HISTORY");

                    if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                        List<String> loanHistory = Arrays.asList(paymentHistory.asText().split("\\|"));
                        totalPayment = loanHistory.size();
                        for (String monthNode : loanHistory) {
                            String date = monthNode.split(",")[0];
                            String code1 = monthNode.split(",")[1].split("/")[0];
                            String code2 = monthNode.split(",")[1].split("/")[1];
                            if(delinquentDPDStatus.contains(code2)){
                                deqPayment+=1;
                            }
                        }
                        onTimePayment = totalPayment - deqPayment;
                    }
                }else if(responses.isArray()){
                    for(JsonNode response: responses){
                        JsonNode paymentHistory = response.get(CrifConstants.LOAN_DETAILS).get("COMBINED-PAYMENT-HISTORY");

                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            List<String> loanHistory = Arrays.asList(paymentHistory.asText().split("\\|"));
                            totalPayment += loanHistory.size();
                            for (String monthNode : loanHistory) {
                                String date = monthNode.split(",")[0];
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                if(delinquentDPDStatus.contains(code2)){
                                    deqPayment+=1;
                                }
                            }
                            onTimePayment += loanHistory.size() - deqPayment;
                        }
                    }
                }
            }

            if(totalPayment!=0){
                timelyPayment = (onTimePayment*100)/totalPayment;
            }

            if(timelyPayment > 90){
                impact = "excellent";
            }else if(timelyPayment > 50 && timelyPayment <= 90){
                impact = "average";
            }else if(timelyPayment <= 50){
                impact = "bad";
            }

            CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
            CreditScoreReportDetailDTO.PaymentHistory paymentHistory = creditScoreReportDetailDTO.new PaymentHistory();

            if(totalPayment == 0){
                return null;
            }

            paymentHistory.setTotalPayment(totalPayment);
            paymentHistory.setOntimePayment(onTimePayment);
            // NEED TO CHECK WHEN NO PAYMENT HISTORY
            paymentHistory.setTimelyPayment(timelyPayment);
            paymentHistory.setImpact(impact);

            return paymentHistory;
        }catch (Exception ex){
            logger.error("Error Occurred while checking payment history Error :{0}", ex);
        }
        return null;
    }

    public CreditScoreReportDetailDTO.AgeOfAccount getAgeOfAccount(JsonNode bureauResponse){

        try{
            boolean responseCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE));
            int averageAge = 0;
            int newestAccount = 0;
            int oldestAccount = 0;
            int currentDiff = 0;
            int totalAge = 0;
            String impact = null;

            if(responseCheck) {
                JsonNode responses = bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);

                if (responses.isObject() && Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS))) {
                    JsonNode loanDetail = responses.get(CrifConstants.LOAN_DETAILS);
                    JsonNode openDate = responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_DT);
                    Date dateReported = null;
                    Date OpenDateType = null;
                    try {
                        if (loanDetail.get(CrifConstants.DATE_REPORTED) != null
                                && !loanDetail.get(CrifConstants.DATE_REPORTED).asText().equalsIgnoreCase("") && Objects.nonNull(openDate) && !openDate.asText().equalsIgnoreCase("")) {
                            dateReported = dateFormat.parse(loanDetail.get(CrifConstants.DATE_REPORTED).asText());
                            OpenDateType = dateFormat.parse(openDate.asText());

                            averageAge =
                                    (int)CommonUtil.getDateDiffInDays(OpenDateType, dateReported) / 365 ;
                            newestAccount = averageAge;
                            oldestAccount = averageAge;
                        }
                    } catch (Exception e) {
                        logger.error("Exception:", e);
                    }
                }else if(responses.isArray()){
                    int total = responses.size();
                    for(JsonNode response : responses){
                        JsonNode loanDetail = response.get(CrifConstants.LOAN_DETAILS);
                        JsonNode openDate = response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_DT);
                        Date dateReported = null;
                        Date OpenDateType = null;
                        try {
                            if (loanDetail.get(CrifConstants.DATE_REPORTED) != null
                                    && !loanDetail.get(CrifConstants.DATE_REPORTED).asText().equalsIgnoreCase("") && Objects.nonNull(openDate) && !openDate.asText().equalsIgnoreCase("")) {
                                dateReported = dateFormat.parse(loanDetail.get(CrifConstants.DATE_REPORTED).asText());
                                OpenDateType = dateFormat.parse(openDate.asText());

                                currentDiff = (int)CommonUtil.getDateDiffInDays(OpenDateType,
                                        dateReported) / 365 ;
                                newestAccount = Math.min(newestAccount == 0 ? Integer.MAX_VALUE : newestAccount , currentDiff);
                                oldestAccount = Math.max( oldestAccount, currentDiff);
                                totalAge += currentDiff;
                            }
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                        }
                    }
                    if(total!= 0){
                        averageAge = totalAge/total;
                    }
                }
            }

            if(averageAge < 1){
                impact = "average";
            }else {
                impact = "excellent";
            }
            CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
            CreditScoreReportDetailDTO.AgeOfAccount ageOfAccount = creditScoreReportDetailDTO.new AgeOfAccount();

            ageOfAccount.setNewestAccount(newestAccount);
            ageOfAccount.setOldestAccount(oldestAccount);
            ageOfAccount.setAverageAge(averageAge);
            ageOfAccount.setImpact(impact);
            return ageOfAccount;
        }catch (Exception ex){
            logger.error("Error Occurred while checking age of account Error :{0}", ex);
        }
        return null;
    }

    public CreditScoreReportDetailDTO.TotalAccount getTotalAccount(JsonNode bureauResponse){

        try{
            boolean responseCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE));

            int totalNumAccount = 0;
            int activeAccount = 0;
            int closedAccount = 0;
            String impact = null;

            if(responseCheck){
                JsonNode responses = bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
                if (responses.isObject() && Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS))) {
                    JsonNode loanDetail = responses.get(CrifConstants.LOAN_DETAILS);
                    if(isLoanClosed(loanDetail)){
                        closedAccount +=1;
                    }
                    totalNumAccount = closedAccount;
                }else if(responses.isArray()){
                    for(JsonNode response : responses){
                        JsonNode loanDetail = response.get(CrifConstants.LOAN_DETAILS);
                        if(isLoanClosed(loanDetail)){
                            closedAccount +=1;
                        }else{
                            activeAccount+=1;
                        }
                    }
                    totalNumAccount = closedAccount + activeAccount;
                }
            }

            if(activeAccount <= 10){
                impact = "excellent";
            }else if(activeAccount <= 20){
                impact = "average";
            }else {
                impact = "bad";
            }

            CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
            CreditScoreReportDetailDTO.TotalAccount totalAccount = creditScoreReportDetailDTO.new TotalAccount();

            if(totalNumAccount==0 && activeAccount==0 && closedAccount==0){
                return null;
            }

            totalAccount.setTotalAccount(totalNumAccount);
            totalAccount.setActiveAccount(activeAccount);
            totalAccount.setClosedAccount(closedAccount);
            totalAccount.setImpact(impact);
            return totalAccount;
        }catch(Exception ex){
            logger.error("Error Occurred while checking total account, Error :{0}", ex);

        }
        return null;
    }

    public CreditScoreReportDetailDTO.CreditEnquries getCreditEnquiries(JsonNode bureauResponse){

        try{
            boolean creditEnquriesCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get("ACCOUNTS-SUMMARY"));

            int totalEnquries = 0;
            int creditCardEnquries= 0;
            int loanEnqueries = 0;
            String impact = null;

            if(creditEnquriesCheck) {
                JsonNode accountSummaries = bureauResponse.get(CrifConstants.REPORT_HEADER).get("ACCOUNTS-SUMMARY");
                if (accountSummaries.isObject()) {
                    JsonNode enquiryReason = accountSummaries.get("DERIVED-ATTRIBUTES");
                    if (Objects.nonNull(enquiryReason)) {
                        creditCardEnquries += enquiryReason.get("INQURIES-IN-LAST-SIX-MONTHS").asLong();
                    }
                } else if (accountSummaries.isArray()) {
                    for (JsonNode accountSummary : accountSummaries) {
                        JsonNode enquiryReason = accountSummaries.get("DERIVED-ATTRIBUTES");
                        if (Objects.nonNull(enquiryReason)) {
                            creditCardEnquries += enquiryReason.get("INQURIES-IN-LAST-SIX-MONTHS").asLong();
                        }
                    }
                }
                totalEnquries = creditCardEnquries + loanEnqueries;
            }

            if(totalEnquries <= 2){
                impact = "excellent";
            }else if(totalEnquries > 3 && totalEnquries <= 6){
                impact = "average";
            }else if(totalEnquries >= 7){
                impact = "bad";
            }

            CreditScoreReportDetailDTO creditScoreReportDetailDTO = new CreditScoreReportDetailDTO();
            CreditScoreReportDetailDTO.CreditEnquries creditEnquries = creditScoreReportDetailDTO.new CreditEnquries();

            creditEnquries.setTotalEnquiries(totalEnquries);
            creditEnquries.setLoanEnquiries(loanEnqueries);
            creditEnquries.setCreditCardEnquiries(creditCardEnquries);
            creditEnquries.setImpact(impact);
            return creditEnquries;
        }catch(Exception ex){
            logger.error("Error Occurred while checking total enquries, Error :{0}", ex);

        }
        return null;

    }

    public String getExperianNumber(JsonNode bureauResponse){
        String experianNumber = null;
        boolean experianHeaderDetails = Objects.nonNull(bureauResponse.get("B2C-REPORT")) && Objects.nonNull(bureauResponse.get("B2C-REPORT").get("HEADER"));
        if(experianHeaderDetails){
            JsonNode headerDetails = bureauResponse.get("B2C-REPORT").get("HEADER");
            if(Objects.nonNull(headerDetails.get("REPORT-ID"))){
                experianNumber = headerDetails.get("REPORT-ID").asText();
            }
        }

        return experianNumber;
    }

    public LoanAndCreditCardDetailDTO getLoanAndCreditDetail(JsonNode bureauResponse){
        try{
            boolean responseCheck = Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES)) && Objects.nonNull(bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE));


            List<LoanAndCreditCardDetailDTO.CreditCardDetail> creditCardDetails = new ArrayList<>();
            List<LoanAndCreditCardDetailDTO.LoanDetail> loanDetails = new ArrayList<>();
            LoanAndCreditCardDetailDTO loanAndCreditCardDetailDTO = new LoanAndCreditCardDetailDTO();

            if(responseCheck) {
                JsonNode responses = bureauResponse.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
                if(responses.isObject()){
                    JsonNode loan = responses.get(CrifConstants.LOAN_DETAILS);
                    LoanAndCreditCardDetailDTO.CreditCardDetail creditCardDetail = loanAndCreditCardDetailDTO.new CreditCardDetail();
                    LoanAndCreditCardDetailDTO.LoanDetail loanDetail = loanAndCreditCardDetailDTO.new LoanDetail();
                    if(Objects.nonNull(loan) && (loan.get(CrifConstants.ACCT_TYPE).asText().equals("Credit Card") || loan.get(CrifConstants.ACCT_TYPE).asText().equals("Corporate Credit Card"))){
                        creditCardDetail.setBankName(loan.get("CREDIT-GUARANTOR").asText());
                        creditCardDetail.setStatus(!isLoanClosed(loan));
                        creditCardDetail.setCreditCardNumber(loan.get("ACCT-NUMBER").asText());
                        if(Objects.nonNull(loan.get("CURRENT-BAL"))) {
                            creditCardDetail.setBalance(Math.max(Integer.parseInt(loan.get("CURRENT-BAL").asText().replace(",", "")), 0));
                        }
                        if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT))){
                            creditCardDetail.setCardLimit(Math.max(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT).asText().replace(",", "")), 0));
                        }else if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                            creditCardDetail.setCardLimit(Math.max(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")), 0));
                        }
                        creditCardDetails.add(creditCardDetail);
                    }else{
                        loanDetail.setAccountNumber(loan.get("ACCT-NUMBER").asText());
                        loanDetail.setBankName(loan.get("CREDIT-GUARANTOR").asText());
                        loanDetail.setStatus(!isLoanClosed(loan));
                        if(Objects.nonNull(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                            loanDetail.setSanctionedAmount(Integer.parseInt(responses.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")));
                        }
                        if(Objects.nonNull(loan.get("ORIGINAL-TERM"))){
                            loanDetail.setTenure(loan.get("ORIGINAL-TERM").asText());
                        }
                        loanDetail.setCurrentBalance(loan.get("CURRENT-BAL").asText());
                        if(Objects.nonNull(loan.get("INTEREST-RATE"))) {
                            loanDetail.setRateOfInterest(loan.get("INTEREST-RATE").asText());
                        }
                        loanDetails.add(loanDetail);
                    }
                }else if(responses.isArray()){
                    for (JsonNode response: responses) {
                        JsonNode loan = response.get(CrifConstants.LOAN_DETAILS);
                        LoanAndCreditCardDetailDTO.CreditCardDetail creditCardDetail = loanAndCreditCardDetailDTO.new CreditCardDetail();
                        LoanAndCreditCardDetailDTO.LoanDetail loanDetail = loanAndCreditCardDetailDTO.new LoanDetail();
                        if(Objects.nonNull(loan) && (loan.get(CrifConstants.ACCT_TYPE).asText().equals("Credit Card") || loan.get(CrifConstants.ACCT_TYPE).asText().equals("Corporate Credit Card"))){
                            creditCardDetail.setBankName(loan.get("CREDIT-GUARANTOR").asText());
                            creditCardDetail.setStatus(!isLoanClosed(loan));
                            creditCardDetail.setCreditCardNumber(loan.get("ACCT-NUMBER").asText());
                            if(Objects.nonNull(loan.get("CURRENT-BAL"))) {
                                creditCardDetail.setBalance(Math.max(Integer.parseInt(loan.get("CURRENT-BAL").asText().replace(",", "")), 0));
                            }
                            if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT))){
                                creditCardDetail.setCardLimit(Math.max(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.CREDIT_LIMIT).asText().replace(",", "")), 0));
                            }else if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                                creditCardDetail.setCardLimit(Math.max(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")), 0));
                            }

                            creditCardDetails.add(creditCardDetail);
                        }else{
                            loanDetail.setAccountNumber(loan.get("ACCT-NUMBER").asText());
                            loanDetail.setBankName(loan.get("CREDIT-GUARANTOR").asText());
                            if(Objects.nonNull(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT))){
                                loanDetail.setSanctionedAmount(Integer.parseInt(response.get(CrifConstants.LOAN_DETAILS).get(CrifConstants.DISBURSED_AMT).asText().replace(",", "")));
                            }
                            if(Objects.nonNull(loan.get("ORIGINAL-TERM"))){
                                loanDetail.setTenure(loan.get("ORIGINAL-TERM").asText());
                            }
                            loanDetail.setStatus(!isLoanClosed(loan));
                            loanDetail.setCurrentBalance(loan.has("CURRENT-BAL") && Objects.nonNull(loan.get("CURRENT-BAL")) ? loan.get("CURRENT-BAL").asText() : null);
                            if(Objects.nonNull(loan.get("INTEREST-RATE"))){
                                loanDetail.setRateOfInterest(loan.get("INTEREST-RATE").asText());
                            }
                            loanDetails.add(loanDetail);
                        }

                    }
                }
            }
            loanAndCreditCardDetailDTO.setLoanDetail(loanDetails);
            loanAndCreditCardDetailDTO.setCreditCardDetail(creditCardDetails);
            loanAndCreditCardDetailDTO.setExperianNumber(getExperianNumber(bureauResponse));
            return loanAndCreditCardDetailDTO;
        }catch(Exception ex){
            logger.error("Error Occurred while checking loan and credit card details, Error :{0}", ex);

        }
        return null;
    }

    @Override
    public int tradeDpdLessThan(int minDpd, int maxDpd, int month) {
        int dpd=0;
        try {
            int count = 0;
            Date dateReported = null;
            Date reportDate = getReportDate();
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while fetching Report Date :{}", e);
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= minDpd && dpd <= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                try {
                    JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                    if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                        dateReported = dateFormat.parse(dateRep.asText());
                    }
                } catch (Exception e) {
                    logger.error("Exception while fetching Report Date :{}", e);
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                    JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                    if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                        String[] loanHistory = paymentHistory.asText().split("\\|");
                        for (String monthNode : loanHistory) {
                            String code1 = monthNode.split(",")[1].split("/")[0];
                            String code2 = monthNode.split(",")[1].split("/")[1];
                            dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                            if (dpd >= minDpd && dpd <= maxDpd) {
                                count++;
                            }
                        }
                    }
                    if (count > 1) {
                        count = 1;
                        dpd = count + dpd;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in tradeDpdLessThan", e);
        }
        return dpd;
    }

    @Override
    public int tradeDpdMoreThan(int minDpd, int maxDpd, int month) {
        int dpd=0;
        try {
            int count = 0;
            Date dateReported = null;
            Date reportDate = getReportDate();
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while fetching Report Date :{}", e);
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= minDpd && dpd <= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                try {
                    JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                    if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                        dateReported = dateFormat.parse(dateRep.asText());
                    }
                } catch (Exception e) {
                    logger.error("Exception while fetching Report Date :{}", e);
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                    JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                    if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                        String[] loanHistory = paymentHistory.asText().split("\\|");
                        for (String monthNode : loanHistory) {
                            String code1 = monthNode.split(",")[1].split("/")[0];
                            String code2 = monthNode.split(",")[1].split("/")[1];
                            dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                            if (dpd >= minDpd && dpd <= maxDpd) {
                                count++;
                            }
                        }
                    }
                    if (count > 1) {
                        count = 1;
                        dpd = count + dpd;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in tradeDpdMoreThan", e);
        }
        return dpd;
    }

    @Override
    public int subDPDMoreThan(int maxDpd, int month) {
        int dpd=0;
        try {
            int count = 0;
            Date dateReported = null;
            Date reportDate = getReportDate();
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while fetching Report Date :{}", e);
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                try {
                    JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                    if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                        dateReported = dateFormat.parse(dateRep.asText());
                    }
                } catch (Exception e) {
                    logger.error("Exception while fetching Report Date :{}", e);
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) > month * 30) {
                    JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                    if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                        String[] loanHistory = paymentHistory.asText().split("\\|");
                        for (String monthNode : loanHistory) {
                            String code1 = monthNode.split(",")[1].split("/")[0];
                            String code2 = monthNode.split(",")[1].split("/")[1];
                            dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                            if (dpd >= maxDpd) {
                                count++;
                            }
                        }
                    }
                    if (count > 1) {
                        count = 1;
                        dpd = count + dpd;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in subDPDMoreThan", e);
        }
        return dpd;
    }

    @Override
    public int subDPDLessThan(int maxDpd, int month) {
        int dpd=0;
        try {
            int count = 0;
            Date dateReported = null;
            Date reportDate = getReportDate();
            JsonNode accounts = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
            if (accounts != null && accounts.isArray()) {
                for (JsonNode account : accounts) {
                    JsonNode loanDetail = account.get(CrifConstants.LOAN_DETAILS);
                    try {
                        JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                        if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                            dateReported = dateFormat.parse(dateRep.asText());
                        }
                    } catch (Exception e) {
                        logger.error("Exception while fetching Report Date :{}", e);
                    }
                    if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                        JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                        if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                            String[] loanHistory = paymentHistory.asText().split("\\|");
                            for (String monthNode : loanHistory) {
                                String code1 = monthNode.split(",")[1].split("/")[0];
                                String code2 = monthNode.split(",")[1].split("/")[1];
                                dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                                if (dpd >= maxDpd) {
                                    count++;
                                }
                            }
                        }
                        if (count > 1) {
                            count = 1;
                            dpd = count + dpd;
                        }
                    }
                }
            } else if (accounts != null && accounts.isObject()) {
                JsonNode loanDetail = accounts.get(CrifConstants.LOAN_DETAILS);
                try {
                    JsonNode dateRep = accounts.get(CrifConstants.DATE_REPORTED);
                    if (dateRep != null && !dateRep.toString().equalsIgnoreCase("\"\"")) {
                        dateReported = dateFormat.parse(dateRep.asText());
                    }
                } catch (Exception e) {
                    logger.error("Exception while fetching Report Date :{}", e);
                }
                if (dateReported != null && CommonUtil.getDateDiffInDays(dateReported, reportDate) < month * 30) {
                    JsonNode paymentHistory = loanDetail.get("COMBINED-PAYMENT-HISTORY");
                    if (paymentHistory != null && !paymentHistory.toString().equalsIgnoreCase("\"\"")) {
                        String[] loanHistory = paymentHistory.asText().split("\\|");
                        for (String monthNode : loanHistory) {
                            String code1 = monthNode.split(",")[1].split("/")[0];
                            String code2 = monthNode.split(",")[1].split("/")[1];
                            dpd = (delinquentDPDStatus.contains(code2)) ? 90 : codeToCount(code1);
                            if (dpd >= maxDpd) {
                                count++;
                            }
                        }
                    }
                    if (count > 1) {
                        count = 1;
                        dpd = count + dpd;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in subDPDLessThan", e);
        }
        return dpd;
    }

    @Override
    public Boolean getETC() {
        try {
            if(getBureauScore() >= 300) {
                return true;
            }
            return false;
//            JsonNode loanDetails = response.get(CrifConstants.REPORT_HEADER).get(CrifConstants.RESPONSES).get(CrifConstants.RESPONSE);
//            if (loanDetails != null && loanDetails.isObject()) {
//                loanDetails = loanDetails.get(CrifConstants.LOAN_DETAILS);
//                if (getLoanAmount(loanDetails) >= 1000 && getBureauScore() >= 300) {//not a credit card
//                    return true;
//                }
//            } else if (loanDetails != null && loanDetails.isArray()) {
//                for (JsonNode detail : loanDetails) {
//                    detail = detail.get(CrifConstants.LOAN_DETAILS);
//                    if (getLoanAmount(detail) >= 1000 && getBureauScore() >= 300) {//not a credit card
//                        return true;
//                    }
//                }
//            }
        } catch (Exception e) {
            logger.error("Exception in getETC in crif", e);
        }
        return false;
    }

}
