package com.github.scheduler.utils;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public class UtilsTest {

    public static final Pattern COMMON_DATE_TIME_PATTERN = Pattern.compile(
            //year    month     day        hour       minute     second       millis  time zone
            "[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}(Z|[+\\-][0-9]{2}(:[0-9]{2}))"
    );

    public static final Pattern DATE_PATTERN = Pattern.compile(
            //year    month     day
            "[0-9]{4}-[01][0-9]-[0-3][0-9]"
    );

    public static final Pattern TIME_PATTERN = Pattern.compile(
            //hour       minute     second       millis  time zone
            "[0-2][0-9]:[0-5][0-9]:[0-5][0-9]"
    );

    public static void main(String[] args){
        // boolean match = Utils.simpleDateFormatMatch("yyyy-MM-dd","2020-02-30");
        // System.out.println("match:" + match);

        // Date date = Utils.dateParse("yyyy-MM-dd","2020-22-30");
        // System.out.println(date.toString());

        // boolean match = DATE_PATTERN.matcher("2021-02-39").matches();

        // boolean match = TIME_PATTERN.matcher("01:1:59").matches();

        // System.out.println("match:" + match);

        Calendar cal = Calendar.getInstance();
        System.out.println(cal.getTimeInMillis());


        LocalTime localTime = LocalTime.parse("01:15:03");
        System.out.println(localTime.toSecondOfDay());

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY,24);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        System.out.println(calendar.getTimeInMillis());

    }
}
