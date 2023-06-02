package com.bharatpe.lending.bureaurefactoring.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BureauClientException extends Exception{
    private Object request; //only for CRIF case
    private String responseBody;
    private Integer httpStatus; //-1 signifies timeout

    public BureauClientException(String message, Integer httpStatus, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
    }

    public BureauClientException(String message, Integer httpStatus, Object request, String responseBody) {
        super(message);
        this.httpStatus = httpStatus;
        this.responseBody = responseBody;
        this.request = request;
    }

    public BureauClientException(String message, Exception e) {
        super(message, e);
    }

}
