package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.*;

public class FixRateJobRunner extends JobRunner{
    private static final Logger LOG = LogManager.getLogger(FixRateJobRunner.class);
    private final long initialDelay;
    private final long period;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private WrappedRunnable runnable;
    private ScheduledFuture<?> future;

    public FixRateJobRunner(ScheduleMode scheduleMode,
                            List<String> cmdList,
                            long initialDelay,
                            long period,
                            TimeUnit timeUnit){
        super(scheduleMode,cmdList);
        this.initialDelay = initialDelay;
        this.period = period;
        this.timeUnit = timeUnit;
    }

    @Override
    public void init(){
        this.shell = new Shell.ShellCommandExecutor(Utils.cmdListToArray(this.cmdList));
        int scheduleCode = this.getScheduleMode().getMode();
        this.runnable = new WrappedRunnable() {
            @Override
            protected JobResponse doWork() throws Exception {
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

                return jobResponse;
            }
        };
    }

    @Override
    public void start() {
        init();
        future = ExecutorServiceUtil.scheduleAtFixedRate(
                    this.executorService,
                    this.runnable,
                    this.initialDelay,
                    this.period,
                    this.timeUnit);
    }

    @Override
    public void waitComplete() {
        /*
        while (true){
            long remainingDelay = future.getDelay(TimeUnit.SECONDS);
            LOG.info("remaining delay:{}",remainingDelay);
            Utils.sleepQuietly(5 * 1000);
        }
        */
    }

    @Override
    public void setResponseHandler(JobResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void stop() {
    }
}
