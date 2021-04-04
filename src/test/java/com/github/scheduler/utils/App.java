package com.github.scheduler.utils;

import java.util.Arrays;

public class App {

    public static void main(String[] args){
        Tuple<String,Long> tuple = Utils.getNextExecuteDayInWeek(Arrays.asList("sunday","wednesday","monday"));
        System.out.println(tuple.v1());
        System.out.println(tuple.v2());

    }
}
