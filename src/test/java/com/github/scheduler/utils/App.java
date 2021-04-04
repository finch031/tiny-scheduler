package com.github.scheduler.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class App {

    public static void main(String[] args){
        // Tuple<String,Long> tuple = Utils.getNextExecuteDayInWeek(Arrays.asList("sunday","wednesday","monday"));
        // System.out.println(tuple.v1());
        // System.out.println(tuple.v2());

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

    }
}
