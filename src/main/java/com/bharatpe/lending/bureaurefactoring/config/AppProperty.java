package com.bharatpe.lending.bureaurefactoring.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
@Component
@Getter
@Setter
public class AppProperty {
    public static final String DEBUG = "enable.debug";

    @Autowired
    Environment env;

    Boolean enableDebug;

    @Value("${experian.voucher}")
    String voucherCode;

    @PostConstruct
    void loadEnvVariables() {
        this.setEnableDebug(Boolean.valueOf(env.getProperty(DEBUG)));
    }
}
