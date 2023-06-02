package com.bharatpe.lending.bureaurefactoring.constants;

import com.bharatpe.lending.bureaurefactoring.enums.BureauLoanType;

import java.util.*;

public interface ExperianConstants {
    enum COLOR {
        RED, AMBER, LIGHT_GREEN, DARK_GREEN;
    }

    String CREDIT_LINE_CATEGORY = "CREDIT_LINE_CATEGORY";
    String INVALID_PANCARD = "INVALID_PANCARD";
    String LOW_BP_SCORE = "LOW_BP_SCORE";
    String ENACH="ENACH";
    String ORGANIZED="ORGANIZED";
    String BUSINESS_CATEGORY="BUSINESS_CATEGORY";
    String FRAUD = "FRAUD";
    String OVERDUE = "OVERDUE";
    String LOW_TPV = "LOW_TPV";
    String VINTAGE = "VINTAGE";
    String CATEGORY_RED = "CATEGORY_RED";
    String OGL = "OGL";
    String COVID = "COVID";
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
    String ASSET_CLASSIFICATION="Asset_Classification";
    String LOW_BBS = "LOW_BBS";
    String LOW_NFI = "LOW_NFI";
    String CAPS_DETAILS = "CAPS_Application_Details";
    String LOW_BBS_VINTAGE = "LOW_BBS_VINTAGE";
    String BLOCKED_PANCARD = "BLOCKED_PANCARD";
    String ACCT_TYPE = "Account_Type";
    String DATE_REPORTED = "Date_Reported";
    String DATE_ADDITION = "DateOfAddition";
    String DATE_CLOSED = "Date_Closed";
    String DATE_OF_BIRTH = "Date_of_birth";
    String DPD = "Days_Past_Due";
    String PRODUCT = "Product";
    String DOR = "Date_of_Request";
    String OPEN_DATE = "Open_Date";
    String PROFILE_RESPONSE = "INProfileResponse";
    String ACCT_HISTORY = "CAIS_Account_History";
    String ACCT_DETAILS = "CAIS_Account_DETAILS";
    String ACC_HOLDER_DETAILS = "CAIS_Holder_Details";
    String ACCT_STATUS = "Account_Status";
    String ACCT = "CAIS_Account";
    String CAPS_SUMMARY = "TotalCAPS_Summary";
    String ACCT_HOLDER_TYPE_CODE = "AccountHoldertypeCode";
    String DATE_FORMAT = "yyyyMMdd";
    String AMOUNT_PAST_DUE = "Amount_Past_Due";
    String ENQUIRY_REASON = "Enquiry_Reason";
    String SETTLED_STATUS = "Written_off_Settled_Status";
    String LOAN_AMOUNT = "Highest_Credit_or_Original_Loan_Amount";
    String CURRENT_BALANCE = "Current_Balance";
    String YELLOW = "YELLOW";
    String LOW_ATS = "LOW_ATS";
    String LOW_BUREAU_SCORE = "LOW_BUREAU_SCORE";
    String PAYMENTS_BANK = "PAYMENTS_BANK";
    String MULTIPLE_PSP_APPS = "MULTIPLE_PSP_APPS";
    String FOS_APP = "FOS_APP";
    String HIGH_LOAN_ENQUIRIES = "HIGH_LOAN_ENQUIRIES";
    String D2R = "D2R";
    String NON_CPV_CITY = "NON_CPV_CITY";
    String LOW_SMS_VINTAGE = "LOW_SMS_VINTAGE";
    String LOW_SMS_DQS = "LOW_SMS_DQS";
    String LOW_SMS_360SCORE = "LOW_SMS_360SCORE";
    String SMS_NEGATIVE_EVENT = "SMS_NEGATIVE_EVENT";
    String LOW_SMS_INCOME = "LOW_SMS_INCOME";
    String LOW_SMS_NFI = "LOW_SMS_NFI";
    String NON_ESSENTIAL_CATEGORY = "NON_ESSENTIAL_CATEGORY";
    String FIRST_NTB_LOAN = "FIRST_NTB_LOAN";
    String CURRENT_APPLICATION = "Current_Application";
    String CURRENT_APPLICATION_DETAILS = "Current_Application_Details";
    String CURRENT_APPLICANT_DETAILS = "Current_Applicant_Details";
    String INCOME_TAX_PAN = "IncomeTaxPan";
    String CAIS_HOLDER_ADDRESS_DETAILS = "CAIS_Holder_Address_Details";
    String FIRST_LINE_OF_ADDRESS = "First_Line_Of_Address_non_normalized";
    String SECOND_LINE_OF_ADDRESS = "Second_Line_Of_Address_non_normalized";
    String THIRD_LINE_OF_ADDRESS = "Third_Line_Of_Address_non_normalized";
    String CITY = "City_non_normalized";
    String POSTAL_CODE = "ZIP_Postal_Code_non_normalized";
    String GENDER_CODE = "Gender_Code";
    String DAYS = "days";

    List<String> RED = Arrays.asList("1","2","13","25");
    List<String> AMBER = Arrays.asList("3","4","5","6","14","15","16","17","18","26","27","28","29","37","38","39","40","41");
    List<String> LIGHT_GREEN = Arrays.asList("7","8","9","10","11","19","20","21","22","30","31","33","34","42","45");

    Map<String,String> COLOR_TO_CATEGORY = new HashMap<String,String>() {{
        put("BBS11", "AMBER");
        put("BBS12", "AMBER");
        put("BBS21", "AMBER");
        put("BBS13", "LIGHT_GREEN");
        put("BBS22", "LIGHT_GREEN");
        put("BBS31", "LIGHT_GREEN");
        put("BBS23", "DARK_GREEN");
        put("BBS32", "DARK_GREEN");
        put("BBS33", "DARK_GREEN");
    }};

    Map<BureauLoanType, List<Integer>> LOAN_TYPE_ACC = new HashMap<BureauLoanType,List<Integer>>() {{
        put(BureauLoanType.CREDIT_CARD, Collections.singletonList(10));
        put(BureauLoanType.TWO_WHEELER, Collections.singletonList(13));
        put(BureauLoanType.GOLD_LOAN, Collections.singletonList(7));
        put(BureauLoanType.CONSUMER_DURABLE, Collections.singletonList(6));
        put(BureauLoanType.PERSONAL_LOAN, Collections.singletonList(5));
        put(BureauLoanType.HOME_LOAN, Collections.singletonList(2));
        put(BureauLoanType.AUTO_LOAN, Collections.singletonList(1));
        put(BureauLoanType.BUSINESS_LOAN, Arrays.asList(51,55,61));
        put(BureauLoanType.EXCLUDED_LOAN, Arrays.asList(11,14,16,18,19,20,32,33,34,36,38,39,43,52,53,54,56,57,58));
        put(BureauLoanType.OTHER_LOAN, Collections.singletonList(0));
    }};

    Map<String,Double> DEBT_EMI = new HashMap<String,Double>() {{
        put("AL",3227D);
        put("PL",3467D);
        put("HL",927D);
        put("BL",1137D);
        put("LAP",1137D);
        put("Other",2275D);
    }};
}
