package com.bharatpe.lending.bureaurefactoring.constants;

public interface BureauConstants {
    String MINIMUM_BUREAU_TIME_GAP="30";
    String BUREAU_TYPE_1 = "EXPERIAN";
    String BUREAU_TYPE_2 = "CRIF";
    String SHORT_API_URL = "https://consumer.experian.in:8443/ECV-P2/content/enhancedMatch.action";
    String EXPERIAN_MASK_API_URL = "https://consumer.experian.in:8443/ECV-P2/content/generateMaskedDeliveryData.action";
    String AUTHENTICATE_DELIVERY_DATA_API_URL = "https://consumer.experian.in:8443/ECV-P2/content/authenticateDeliveryData.action";
    String REFRESH_API_URL = "https://consumer.experian.in:8443/ECV-P2/content/onDemandRefresh.action";
    String CLIENT_NAME = "BHARATPE_EM";
    String AUTHORIZED_TO_CALL_MASK_MOBILE_API= "Authorized to Call Masked Mobile API";
    String REPORT_HEADER = "B2C-REPORT";
    String PERSONAL_VARIATIONS = "PERSONAL-INFO-VARIATION";
    String PAN_VARIATIONS = "PAN-VARIATIONS";
    String VARIATION = "VARIATION";
    String PHONE_VARIATIONS = "PHONE-NUMBER-VARIATIONS";
    String BUREAU_DATA_SHARD_KEY = "mobile";
    String EXPERIAN_HIT_ID = "/global_limit/hit_id";
}
