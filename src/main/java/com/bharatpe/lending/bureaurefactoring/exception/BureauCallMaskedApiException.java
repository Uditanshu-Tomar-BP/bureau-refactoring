package com.bharatpe.lending.bureaurefactoring.exception;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BureauCallMaskedApiException extends Exception {
    private JsonNode responseBody;

    public BureauCallMaskedApiException(String message, JsonNode responseBody){
        super(message);
        this.responseBody = responseBody;
    }
}
