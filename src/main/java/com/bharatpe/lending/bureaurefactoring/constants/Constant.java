package com.bharatpe.lending.bureaurefactoring.constants;

import java.util.HashMap;
import java.util.Map;

public class Constant {
    public static final String REQUEST_ID = "requestId";
    public static final String SUCCESS = "success";
    public static final String MESSAGE = "message";
    public static final String DEBUG_MESSAGE = "debug_message";
    public static final String DATA = "data";
    public static final String STATUS_CODE = "status_code";
    public static final String COUNTRY_CODE = "91";
    public static final String MODULE_NAME = "TP_CLUB";
    public static final String HEADER_CLIENT_NAME = "clientName";
    public static final String HEADER_HASH = "hash";
    public static final String TOKEN = "token";
    public static final String STATUS = "status";
    public static final String RESPONSE = "response";
    public static final String RESULT = "result";
    public static final String HEADER_MID_KEY = "mid";
    public static final String POSTPE_WHITELIST_MOBILE = "postpe_whitelist_mobile";
    public static final String POSTPE_WHITELIST_PANCARD = "postpe_whitelist_pancard";
    public static final String BUREAU_COLLECTION = "consumer_bureau_data";
    public static final String POSTPE_COLLECTION = "postpe_limit";
    public static final String CONSUMER_LIMIT_COLLECTION = "consumer_limit";
    public static final String CONSUMER_LIMIT_NEW_COLLECTION = "consumer_limit_new";
    public static final Integer THRESHOLD_CREDIT_CARD_INCOME_POSTPE = 200000;
    public static final Double CREDIT_CARD_INCOME_POSTPE_MULTIPLIER = 0.2;

    public static Map<String,Double> EMI = new HashMap<String,Double>() {{
        put("AL",0.025D);
        put("PL",0.035D);
        put("HL",0.01D);
        put("BL",0.05D);
        put("CC",0.05D);
        put("TW",0.02D);
        put("CD",0.03D);
        put("GL",0.02D);
        put("Other",0.03D);
    }};

    public static Map<String,Double> DBI = new HashMap<String,Double>() {{
        put("AL",0.65D);
        put("PL",0.55D);
        put("HL",0.65D);
        put("BL",0.55D);
        put("CC",0.70D);
        put("TW",0.75D);
        put("CD",0.75D);
        put("GL",1D);
        put("Other",0.55D);
    }};

    public static Map<String,Double> OTHER_INCOME = new HashMap<String,Double>() {{
        put("HL",30000D);
        put("AL",20000D);
        put("PL",20000D);
        put("BL",20000D);
        put("CC",20000D);
        put("TW",15000D);
        put("CD",10000D);
        put("GL",10000D);
        put("Other",10000D);
    }};

    private Constant() {
    }

    public static Map<String,Double> EMI_PROPORTION = new HashMap<String,Double>() {{
        put("AL",0.0225D);
        put("PL",0.035D);
        put("HL",0.01D);
        put("BL",0.05D);
        put("CC",0.05D);
        put("TW",0.02D);
        put("CD",0.03D);
        put("GL",0.02D);
        put("Other",0.03D);
    }};

    public static Map<String,Double> DBI_PROPORTION = new HashMap<String,Double>() {{
        put("AL",0.65D);
        put("PL",0.55D);
        put("HL",0.65D);
        put("BL",0.55D);
        put("CC",0.70D);
        put("TW",0.75D);
        put("CD",0.75D);
        put("GL",0D);
        put("Other",0D);
    }};

    public static Map<String,Double> DBI_PROPORTION_PP = new HashMap<String,Double>() {{
        put("AL",0.65D);
        put("PL",0.55D);
        put("HL",0.65D);
        put("BL",0.55D);
        put("CC",0.20D);
        put("TW",0.75D);
        put("CD",0.75D);
        put("GL",0D);
        put("Other",0D);
    }};

    public static Map<String,Boolean> LOAN_CONSIDERED = new HashMap<String,Boolean>() {{
        put("AL",true);
        put("PL",true);
        put("HL",true);
        put("BL",true);
        put("CC",true);
        put("TW",true);
        put("CD",true);
        put("GL",true);
        put("Other",true);
    }};
}
