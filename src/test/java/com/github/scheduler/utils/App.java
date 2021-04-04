package com.github.scheduler.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class App {

    public static void appendPosixTime(StringBuilder buf, long transMillis) {
        int millis = 0;

        if(transMillis > 86400000L){
            long temp = transMillis % 86400000L;

            long c = (transMillis - temp) / 86400000L;
            System.out.println("days:" + c);

            // millis = (int)temp;
            millis = (int)(transMillis - c * 86400000L);
        }

        long a = transMillis / 86400000L;
        System.out.println(a);
        System.out.println(89900456L / 86400000L);

        if (millis < 0) {
            buf.append('-');
            millis = -millis;
        }
        int hours = millis / 3600000;
        buf.append(hours);
        millis -= hours * 3600000;
        if (millis == 0) {
            return;
        }
        buf.append(':');
        int minutes = millis / 60000;
        if (minutes < 10) {
            buf.append('0');
        }
        buf.append(minutes);
        millis -= minutes * 60000;
        if (millis == 0) {
            return;
        }
        buf.append(':');
        int seconds = millis / 1000;
        if (seconds < 10) {
            buf.append('0');
        }
        buf.append(seconds);
    }

    public static void main(String[] args){
        Tuple<String,Long> tuple = Utils.getNextExecuteDayInWeek(Arrays.asList("tuesday","saturday","thursday"));
        System.out.println(tuple.v1());
        System.out.println(tuple.v2());

        List<String> datesList = new ArrayList<>();
        datesList.add("2021-03-15");
        datesList.add("2021-06-03");
        datesList.add("2021-04-05");
        datesList.add("2021-04-18");

        for (String s : datesList) {
            System.out.println(s);
        }

        System.out.println("- - - - - - - - -");

        datesList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        for (String s : datesList) {
            System.out.println(s);
        }

        System.out.println("- - - - - - - - -");

        String currDate = Utils.currentDate();
        for (String s : datesList) {
            if(s.compareTo(currDate) > 0){
                System.out.println("next execute date:" + s);
                break;
            }
        }

        System.out.println(Utils.dateParse("yyyy-MM-dd","2021-04-05").getTime());

        System.out.println(Utils.getNextExecuteDate(Arrays.asList("2021-04-04","2021-04-05","2021-01-04","2021-05-12")));

        long a = 3542366496L;
        StringBuilder sb = new StringBuilder();
        appendPosixTime(sb,a);
        System.out.println(sb.toString());
    }
}
