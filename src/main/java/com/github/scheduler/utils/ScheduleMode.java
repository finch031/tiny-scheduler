package com.github.scheduler.utils;

/**
 * @author yusheng
 * @version 0.0.1
 * @datetime 2021-03-28 21:49
 * @description 调度模式.
 * */
public enum ScheduleMode {
    /**
     * 单次执行.
     * */
    ONCE("ONCE",1),

    /**
     * 每隔指定时间执行一次.
     * */
    AT_FIXED_RATE("AT_FIXED_RATE",2),

    /**
     * 每日指定时间点执行一次.
     * */
    DAILY_FIXED_TIME("DAILY_FIXED_TIME",3),

    /**
     * 每个星期x1、星期x2、星期x3等的指定时间点执行一次.
     * */
    DAY_IN_WEEK_FIXED_TIME("DAY_IN_WEEK_FIXED_TIME",4),

    /**
     * 指定的日期1、日期2、日期3等的指定时间点执行一次.
     * */
    DATE_FIXED_TIME("DATE_FIXED_TIME",5);

    private final String name;
    private final int mode;

    ScheduleMode(String name,int mode){
        this.name = name;
        this.mode = mode;
    }

    public String getName() {
        return this.name;
    }

    public int getMode(){
        return this.mode;
    }
}
