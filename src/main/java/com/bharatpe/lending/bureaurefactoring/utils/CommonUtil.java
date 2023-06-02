package com.bharatpe.lending.bureaurefactoring.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Slf4j
@Component
public class CommonUtil {
    @Autowired
    ObjectMapper objectMapper;

    public static long getDateDiffInDays(Date startTime, Date endTime) {
        long diff = endTime.getTime() - startTime.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
    }


    public static List<JsonNode> jsonNodeArrayUtil(JsonNode nodeData){
        List<JsonNode> resp = new ArrayList<>();
        if(nodeData != null && !nodeData.asText().equals("\"\"")){
            if(nodeData.isObject()){
                resp.add(nodeData);
            } else {
                for(JsonNode node: nodeData){
                    resp.add(node);
                }
            }
        }
        return resp;
    }

    public static int getDateDiffInYears(Date first, Date last) {
        Calendar a = getCalendar(first);
        Calendar b = getCalendar(last);
        int diff = b.get(Calendar.YEAR) - a.get(Calendar.YEAR);
        if (a.get(Calendar.DAY_OF_YEAR) > b.get(Calendar.DAY_OF_YEAR)) {
            diff--;
        }
        return diff;
    }

    public static boolean isPersonalPan(String pancard) {
        String fourthLetter = pancard.substring(3,4);
        return fourthLetter.equalsIgnoreCase("P");
    }

    public static Calendar getCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public static int getDateDiffInMonths(Date d1, Date d2){
        if(d2==null || d1==null){
            return -1;//Error
        }
        Calendar m_calendar=Calendar.getInstance();
        m_calendar.setTime(d1);
        int nMonth1=12*m_calendar.get(Calendar.YEAR)+m_calendar.get(Calendar.MONTH);
        m_calendar.setTime(d2);
        int nMonth2=12*m_calendar.get(Calendar.YEAR)+m_calendar.get(Calendar.MONTH);
        return Math.abs(nMonth2-nMonth1);
    }


    public static double max(Double... value) {
        double max = 0;
        for (Double val : value) {
            max = Math.max(max, val);
        }
        return max;
    }
}
