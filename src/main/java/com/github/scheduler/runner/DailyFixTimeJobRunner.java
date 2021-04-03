package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.DailyJobThread;
import com.github.scheduler.utils.ScheduleMode;
import com.github.scheduler.utils.Shell;
import com.github.scheduler.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DailyFixTimeJobRunner extends JobRunner{
    private static final Logger LOG = LogManager.getLogger(DailyFixTimeJobRunner.class);
    private final String executeTime;
    private boolean jobExecuteFlag;

    public DailyFixTimeJobRunner(ScheduleMode scheduleMode, List<String> cmdList,String executeTime){
        super(scheduleMode,cmdList);
        this.executeTime = executeTime;
    }

    @Override
    public void init(){
        this.shell = new Shell.ShellCommandExecutor(Utils.cmdListToArray(this.cmdList));
        this.jobExecuteFlag = false;
    }

    @Override
    public void start() {
        init();
        int scheduleCode = this.getScheduleMode().getMode();
        long theSecondOfDay = Utils.theSecondOfDay(executeTime) * 1000L;

        long preDailyStartTimeStamp = Utils.dailyStartTimeStamp();

        while (true){
            long dailyExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;

            if(!jobExecuteFlag && System.currentTimeMillis() >= dailyExecuteTimeStamp){
                DailyJobThread dailyJobThread = new DailyJobThread("daily_fixed_time_job") {
                    @Override
                    public void run() {
                        try{
                            // do real job by shell.
                            shell.execute();

                            long tid = Thread.currentThread().getId();
                            String id = Utils.createJobId(scheduleCode,tid);
                            int retCode = shell.getExitCode();
                            String output = shell.getOutput();
                            String error = shell.getError();
                            JobResponse jobResponse = new JobResponse(id);
                            jobResponse.setRetCode(retCode);
                            jobResponse.setOutput(output);
                            jobResponse.setError(error);
                            handler.handler(jobResponse);

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };

                dailyJobThread.start();
                jobExecuteFlag = true;
            }else{
                long nextExecuteTimeStamp = Utils.nextDayStartTimeStamp() + theSecondOfDay;

                if(!jobExecuteFlag){
                    nextExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;
                }

                long millsDelta = nextExecuteTimeStamp - System.currentTimeMillis();

                StringBuilder sb = new StringBuilder();
                Utils.appendPosixTime(sb,(int)millsDelta);
                LOG.info("time to wait before next execute: {}",sb.toString());

                Utils.sleepQuietly(60 * 1000L);
            }

            // 跨天重置.
            if(Utils.dailyStartTimeStamp() > preDailyStartTimeStamp){
                preDailyStartTimeStamp = Utils.dailyStartTimeStamp();
                jobExecuteFlag = false;
            }
        }
    }

    @Override
    public void setResponseHandler(JobResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void waitComplete() {
    }

    @Override
    public void stop() {
    }
}
