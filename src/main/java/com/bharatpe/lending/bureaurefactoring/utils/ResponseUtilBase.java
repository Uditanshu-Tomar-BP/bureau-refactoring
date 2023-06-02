package com.bharatpe.lending.bureaurefactoring.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseUtilBase {
    protected Logger logger = LoggerFactory.getLogger(ResponseUtilBase.class);

    protected JsonNode response;
    protected String type;
    protected String rejectReason;

    public void setResponse(JsonNode response) {
        this.response = response;
    }

    public void setType(String type) {
        this.type = type;
    }
}
