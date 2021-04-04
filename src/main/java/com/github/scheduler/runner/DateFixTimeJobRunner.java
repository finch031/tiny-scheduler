package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DateFixTimeJobRunner extends JobRunner{
    private static final Logger LOG = LogManager.getLogger(DateFixTimeJobRunner.class);
    private final String executeTime;
    private final List<String> datesList;
    private boolean jobExecuteFlag;

    public DateFixTimeJobRunner(ScheduleMode scheduleMode, List<String> cmdList, String executeTime,List<String> datesList){
        super(scheduleMode,cmdList);
        this.executeTime = executeTime;
        this.datesList = datesList;
    }

    @Override
    public void init(){
        this.shell = new Shell.ShellCommandExecutor(Utils.cmdListToArray(this.cmdList));
        this.jobExecuteFlag = false;
    }

    @Override
    public void setResponseHandler(JobResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void start() {
        init();
        int scheduleCode = this.getScheduleMode().getMode();
        long theSecondOfDay = Utils.theSecondOfDay(executeTime) * 1000L;

        long preDailyStartTimeStamp = Utils.dailyStartTimeStamp();

        while (true){
            String currentDate = Utils.currentDate();

            if(datesList.contains(currentDate)){
                long dailyExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;

                if(!jobExecuteFlag && System.currentTimeMillis() >= dailyExecuteTimeStamp){
                    DailyJobThread dailyJobThread = new DailyJobThread("date_fix_time_job") {
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
                    String nextExecuteDate = Utils.getNextExecuteDate(datesList);
                    if(nextExecuteDate == null){
                        LOG.info("no more job to execute in dates:{}!",Utils.datesListAsString(datesList));
                        break;
                    }

                    long nextExecuteTimeStamp = Utils.dateParse("yyyy-MM-dd",nextExecuteDate).getTime() + theSecondOfDay;
                    if(!jobExecuteFlag){
                        nextExecuteTimeStamp = Utils.dailyStartTimeStamp() + theSecondOfDay;
                    }

                    long millsDelta = nextExecuteTimeStamp - System.currentTimeMillis();
                    StringBuilder sb = new StringBuilder();

                    Utils.appendPosixTime(sb,millsDelta);
                    LOG.info("time to wait before next execute: {}",sb.toString());

                    Utils.sleepQuietly(60 * 1000L);
                }
            }else{
                String nextExecuteDate = Utils.getNextExecuteDate(datesList);
                if(nextExecuteDate == null){
                    LOG.info("no more job to execute in dates:{}!",Utils.datesListAsString(datesList));
                    break;
                }

                long nextExecuteTimeStamp = Utils.dateParse("yyyy-MM-dd",nextExecuteDate).getTime() + theSecondOfDay;
                long millsDelta = nextExecuteTimeStamp - System.currentTimeMillis();
                StringBuilder sb = new StringBuilder();
                Utils.appendPosixTime(sb,millsDelta);
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
    public void waitComplete() {
    }

    @Override
    public void stop() {
    }
}
