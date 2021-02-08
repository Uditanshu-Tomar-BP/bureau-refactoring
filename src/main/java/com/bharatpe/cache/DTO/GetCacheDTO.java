package com.bharatpe.cache.DTO;

import java.util.Objects;

public class GetCacheDTO {

    private Boolean success;
    private Object response;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }
}
