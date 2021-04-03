package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.ScheduleMode;
import com.github.scheduler.utils.Shell;
import com.github.scheduler.utils.Utils;

import java.util.List;

public class DailyFixTimeJobRunner extends JobRunner{
    private final String executeTime;

    public DailyFixTimeJobRunner(ScheduleMode scheduleMode, List<String> cmdList,String executeTime){
        super(scheduleMode,cmdList);
        this.executeTime = executeTime;
    }


    @Override
    public void init(){
        this.shell = new Shell.ShellCommandExecutor(Utils.cmdListToArray(this.cmdList));
        int scheduleCode = this.getScheduleMode().getMode();

    }

    @Override
    public void start() {

    }

    @Override
    public void responseHandler(JobResponseHandler handler) {

    }

    @Override
    public void waitComplete() {

    }

    @Override
    public void stop() {

    }
}
