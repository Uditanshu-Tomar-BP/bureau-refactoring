package com.bharatpe.cache.DTO;

import java.util.Date;
import java.util.Objects;

public class AddCacheDto {
    private String key;

    private Object value;

    private Integer ttl;

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

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public boolean validate(){
        if(Objects.nonNull(this.key) && Objects.nonNull(this.value) && Objects.nonNull(this.ttl)){
            return true;
        }
        return false;
    }
}
