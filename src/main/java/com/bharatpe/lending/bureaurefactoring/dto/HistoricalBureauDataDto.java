package com.bharatpe.lending.bureaurefactoring.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Date;

public class HistoricalBureauDataDto {
    String bureau;
    String pancard;
    String hitId;
    JsonNode bureauResponse;
    Boolean isRefreshed;
    Date reportDate;
}
