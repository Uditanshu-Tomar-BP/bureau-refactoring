package com.bharatpe.lending.bureaurefactoring.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BureauParsingException extends Exception {
    private String rawResponse;
    private Integer httpStatus;

    public BureauParsingException(String message, String rawResponse) {
        super(message);
        this.rawResponse = rawResponse;
    }

    public BureauParsingException(String message, Integer httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public BureauParsingException(String message, Exception e) {
        super(message, e);
    }

}
