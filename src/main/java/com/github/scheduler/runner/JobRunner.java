package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.ScheduleMode;
import com.github.scheduler.utils.Shell;
import java.util.List;

public abstract class JobRunner {
    protected final ScheduleMode scheduleMode;
    protected final List<String> cmdList;
    protected Shell.ShellCommandExecutor shell;

    protected abstract void init();

    public abstract void start();

    public abstract void waitComplete();

    public abstract void responseHandler(JobResponseHandler handler);

    public abstract void stop();

    public JobRunner(ScheduleMode scheduleMode,List<String> cmdList){
        this.scheduleMode = scheduleMode;
        this.cmdList = cmdList;
    }

    public ScheduleMode getScheduleMode(){
        return this.scheduleMode;
    }

    public List<String> getCmdList(){
        return this.cmdList;
    }

    public String printCmdList(){
        StringBuilder sb = new StringBuilder();
        for (String s : this.cmdList) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    public Shell getShell(){
        return this.shell;
    }

}
