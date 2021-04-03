package com.github.scheduler.utils;

public class CurrentDateTime {
    private long timeStamp;

    public CurrentDateTime(long timeStamp){
        this.timeStamp = timeStamp;
    }

    public void getDayOfWeek(){}

    public long currentTimeStamp(){
        return this.timeStamp;
    }
}
