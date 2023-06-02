package com.bharatpe.lending.bureaurefactoring.constants;

import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;

import java.util.*;

public interface CrifConstants {
    enum COLOR {
        RED, AMBER, LIGHT_GREEN, DARK_GREEN;
    }

    String STAGE_1 = "Stage 1";
    String STAGE_2 = "Stage 2";
    String STAGE_3 = "Stage 3";
    String INQUIRY_DATE = "INQUIRY-DATE";
    String INQUIRY_HISTORY = "INQUIRY-HISTORY";
    String LOAN_DETAILS = "LOAN-DETAILS";
    String HISTORY = "HISTORY";
    String REQUEST = "REQUEST";
    String RESPONSES = "RESPONSES";
    String RESPONSE = "RESPONSE";
    String SCORES = "SCORES";
    String DISBURSED_DT = "DISBURSED-DT";
    String DISBURSED_AMT = "DISBURSED-AMT";
    String CREDIT_LIMIT = "CREDIT-LIMIT";
    String CLOSED_DATE = "CLOSED-DATE";
    String DATE_REPORTED = "DATE-REPORTED";
    String EMPLOYMENT_DETAILS = "EMPLOYMENT-DETAILS";
    String EMPLOYMENT_DETAIL = "EMPLOYMENT-DETAIL";
    String SCORE = "SCORE";
    String DATE_FORMAT = "dd-MM-yyyy";
    String ACCT_TYPE = "ACCT-TYPE";
    String ACCT_STATUS = "ACCOUNT-STATUS";
    String REPORT_HEADER = "B2C-REPORT";
    String PERSONAL_VARIATIONS = "PERSONAL-INFO-VARIATION";
    String PAN_VARIATIONS = "PAN-VARIATIONS";
    String VARIATION = "VARIATION";
    String PHONE_VARIATIONS = "PHONE-NUMBER-VARIATIONS";
    String DOB_VARIATIONS = "DATE-OF-BIRTH-VARIATIONS";
    String ADDRESS_VARIATIONS = "ADDRESS-VARIATIONS";
    String CLIENT_NAME = "BHARATPE_EM";
    String VOUCHER_CODE = "BharatPe214K2";
    String CREDIT_LINE_CATEGORY = "CREDIT_LINE_CATEGORY";
    String INVALID_PANCARD = "INVALID_PANCARD";
    String LOW_BP_SCORE = "LOW_BP_SCORE";
    String ENACH="ENACH";
    String BUSINESS_CATEGORY="BUSINESS_CATEGORY";
    String FRAUD = "FRAUD";
    String OVERDUE = "OVERDUE";
    String OVERDUE_AMT = "OVERDUE-AMT";
    String WRITE_OFF_AMT = "WRITE-OFF-AMT ";
    String WRITTEN_OFF_SETTLED_STATUS="WRITTEN-OFF_SETTLED-STATUS";
    String LOW_TPV = "LOW_TPV";
    String VINTAGE = "VINTAGE";
    String CATEGORY_RED = "CATEGORY_RED";
    String OGL = "OGL";
    String NTC = "NTC";
    String TIMEOUT = "TIMEOUT";
    String DEROG_ACCOUNT_STATUS = "Loan default / partial settlement";
    String DEROG_DPD_LAST_3_MONTHS = "Late repayment (5+ days) in last 3 months";
    String DEROG_DPD_LAST_6_MONTHS = "Late repayment (30+ days) in last 6 months";
    String DEROG_DPD_LAST_24_MONTHS = "Late repayment (90+ days) in last 24 months";
    String DEROG_DPD_OLDER_THAN_24_MONTHS = "Late repayment (60+ days) in older than 2 year loans";
    String DEROG_DPD_LAST_12_MONTHS = "Late repayment (60+ days) in last 12 months";
    String DEROG_UNSECURED_LOAN_ENQUIRY = "High unsecured loan enquiries";
    String DEROG_UNSECURED_LOANS = "More than 3 Unsecured Loans";
    String DEROG_MORE_THAN_6_LOAN_ENQUIRY = "High number of loan enquiries in last 6 months";
    List<String> UNSECURED_ACCT_TYPES = Arrays.asList("education loan", "leasing", "personal loan", "consumer loan", "loan to professional", "credit card", "charge card", "fleet card", "overdraft", "od on savings account", "business loan general", "business loan priority sector small business", "business loan priority sector agriculture", "business loan priority sector others", "business non-funded credit facility general", "business non-funded credit facility-priority sector- small business", "business non-funded credit facility-priority sector-agriculture", "business non-funded credit facility-priority sector-others", "telco wireless", "telco broadband", "telco landline", "microfinance business loan", "microfinance personal loan", "microfinance others", "loan on credit card", "prime minister jaan dhan yojana - overdraft", "mudra loans – shishu / kishor / tarun", "business loan unsecured", "jlg individual", "jlg group", "individual", "shg group", "shg individual", "shg group – govt", "shd intra - group", "other");
    List<String> UNSECURED_PRODUCTS = Arrays.asList("personal loan", "credit card", "kisan credit card", "loan on credit card", "prime minister jaan dhan yojana - overdraft", "mudra loans – shishu / kishor / tarun", "microfinance others", "business loan general", "business loan priority sector small business", "business loan priority sector agriculture", "business loan priority sector others", "business non-funded credit facility general", "business non-funded credit facility-priority sector- small business", "business non-funded credit facility-priority sector-agriculture", "business non-funded credit facility-priority sector-others", "staff loan", "business loan unsecured");

    //unsecured loans to consider to calculate active usl principle outstanding amount
    List<String> UNSECURED_LOAN_FOR_POS = Arrays.asList("education loan", "personal loan", "consumer loan", "loan to professional", "credit card", "charge card", "fleet card", "overdraft", "od on savings account", "business loan general", "business loan priority sector small business", "business loan priority sector agriculture", "business loan priority sector others", "business non-funded credit facility general", "business non-funded credit facility-priority sector- small business", "business non-funded credit facility-priority sector-agriculture", "business non-funded credit facility-priority sector-others", "microfinance business loan", "microfinance personal loan", "microfinance others", "loan on credit card", "prime minister jaan dhan yojana - overdraft", "mudra loans – shishu / kishor / tarun", "business loan unsecured", "jlg individual", "jlg group", "individual", "shg group", "shg individual", "shg group – govt", "shd intra - group");
    //TODO verify
    Map<BureauLoanType, List<String>> LOAN_TYPE_ACC = new HashMap<BureauLoanType,List<String>>() {{
        put(BureauLoanType.CREDIT_CARD, Arrays.asList("corporate credit card", "credit card", "secured credit card", "Credit Card"));
        put(BureauLoanType.TWO_WHEELER, Collections.singletonList("Two-Wheeler Loan"));
        put(BureauLoanType.GOLD_LOAN, Collections.singletonList("Gold Loan"));
        put(BureauLoanType.CONSUMER_DURABLE, Collections.singletonList("Consumer Loan"));
        put(BureauLoanType.PERSONAL_LOAN, Arrays.asList("loan against shares / securities", "loan to professional",
                "microfinance personal loan", "mudra loans – shishu / kishor / tarun", "personal loan", "staff loan", "Personal Loan"));
        put(BureauLoanType.HOME_LOAN, Arrays.asList("housing loan", "microfinance housing loan", "property loan", "Housing Loan"));
        put(BureauLoanType.AUTO_LOAN, Arrays.asList("auto loan (personal)", "used car loan", "Auto Loan (Personal)"));
        put(BureauLoanType.BUSINESS_LOAN, Arrays.asList("business loan - secured", "business loan against bank deposits",
                "business loan general", "business loan priority sector agriculture",
                "business loan priority sector others", "business loan priority sector small business",
                "business loan unsecured", "microfinance business loan", "od on savings account", "Business Loan General"));
        put(BureauLoanType.OTHER_LOAN, Collections.singletonList("Other"));
        put(BureauLoanType.EXCLUDED_LOAN, Arrays.asList("Leasing","Non Funded Credit Facility",
                "Fleet Card", "Telco Wireless", "Telco Broadband", "Telco Landline", "Used Car Loan",
                "Consturction Equiment Loan", "Used Tractor Loan", "Kisan Credit Card",
                "business non-funded credit facility-priority sector-others","used car loan",
                "construction equipment loan","tractor loan","kisan credit card",
                "Prime Minister Jaan Dhan Yojana - Overdraft","Mudra Loans – Shishu / Kishor / Tarun",
                "Microfinance Others","business loan priority sector small business",
                "Business Loan Priority Sector Small Business","Business Loan Priority Sector Agriculture",
                "Business Loan Priority Sector Others", "Business Non-Funded Credit Facility-Priority Sector-Others",
                "Business Non-Funded Credit Facility-Priority Sector- Small Business","Business Non-Funded Credit Facility-Priority Sector-Agriculture"));
    }};

}
