package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DayInWeekJobRunner extends JobRunner{
    private static final Logger LOG = LogManager.getLogger(DayInWeekJobRunner.class);
    private final String executeTime;
    private final List<String> dayInWeeksList;
    private boolean jobExecuteFlag;

    public DayInWeekJobRunner(ScheduleMode scheduleMode, List<String> cmdList, String executeTime,List<String> dayInWeeksList){
        super(scheduleMode,cmdList);
        this.executeTime = executeTime;
        this.dayInWeeksList = dayInWeeksList;
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

        while(true){
            String currentDayInWeek = Utils.currentDayInWeek();

            if(dayInWeeksList.contains(currentDayInWeek)){
                long dailyExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;

                if(!jobExecuteFlag && System.currentTimeMillis() >= dailyExecuteTimeStamp){
                    DailyJobThread dailyJobThread = new DailyJobThread("day_in_week_job") {
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
                    Tuple<String,Long> tuple = Utils.getNextExecuteDayInWeek(dayInWeeksList);
                    long nextExecuteTimeStamp = tuple.v2() + Utils.dailyStartTimeStamp() + theSecondOfDay;

                    if(!jobExecuteFlag){
                        nextExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;
                    }

                    long millsDelta = nextExecuteTimeStamp - System.currentTimeMillis();

                    StringBuilder sb = new StringBuilder();
                    Utils.appendPosixTime(sb,(int)millsDelta);
                    LOG.info("time to wait before next execute: {}",sb.toString());

                    Utils.sleepQuietly(60 * 1000L);
                }
            }else{
                Tuple<String,Long> tuple = Utils.getNextExecuteDayInWeek(dayInWeeksList);
                long nextExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;
                long millsDelta = tuple.v2() + (nextExecuteTimeStamp - System.currentTimeMillis());
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
