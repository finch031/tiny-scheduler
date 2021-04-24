package com.github.scheduler.runner;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.utils.ExecutorServiceUtil;
import com.github.scheduler.utils.ScheduleMode;
import com.github.scheduler.utils.Shell;
import com.github.scheduler.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.*;

public class OnceJobRunner extends JobRunner{
    private static final Logger LOG = LogManager.getLogger(OnceJobRunner.class);
    private final long delay;
    private final TimeUnit timeUnit;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private Callable<JobResponse> callable;
    private ExecutorServiceUtil.CallableTaskWrapper<JobResponse> wrapper;
    private JobResponse jobResponse;

    public OnceJobRunner(ScheduleMode scheduleMode,
                         List<String> cmdList,
                         long delay,
                         TimeUnit timeUnit){
        super(scheduleMode,cmdList);
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public long getDelay(){
        return this.delay;
    }

    public TimeUnit getTimeUnit(){
        return this.timeUnit;
    }

    @Override
    protected void init() {
        this.shell = new Shell.ShellCommandExecutor(Utils.cmdListToArray(this.cmdList));
        int scheduleCode = this.getScheduleMode().getMode();
        callable = new Callable<JobResponse>() {
            @Override
            public JobResponse call() throws Exception {
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
                return jobResponse;
            }
        };
    }

    @Override
    public void start() {
        init();
        wrapper = ExecutorServiceUtil.schedule(executorService,callable,this.delay,this.timeUnit);
    }

    @Override
    public void waitComplete() {
        try{
            wrapper.waitTillDone();
            jobResponse = wrapper.get();
        }catch (Exception ex){
            String errorMsg = Utils.stackTrace(ex);
            LOG.error(errorMsg);
        }

        handler.handler(jobResponse);
    }

    @Override
    public void setResponseHandler(JobResponseHandler handler) {
        this.handler = handler;
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }
}
