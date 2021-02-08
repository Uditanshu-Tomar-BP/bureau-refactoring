package com.bharatpe.cache.DTO;

import java.util.Objects;

public class AddCacheDto {
    private String key;

    private Object value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean validate(){
        if(Objects.nonNull(this.key) && Objects.nonNull(this.value)){
            return true;
        }
        return false;
    }
}
