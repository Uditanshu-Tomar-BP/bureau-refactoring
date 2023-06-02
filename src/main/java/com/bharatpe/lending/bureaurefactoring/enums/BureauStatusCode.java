package com.bharatpe.lending.bureaurefactoring.enums;

public enum BureauStatusCode {
    READ_TIME_OUT(-1),
    PARSE_ERROR(-2),
    SECURITY_QUESTION(-3),
    INVALID_REPORT(-4);

    private Integer code;

    BureauStatusCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
