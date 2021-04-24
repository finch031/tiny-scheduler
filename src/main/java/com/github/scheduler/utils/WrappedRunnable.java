package com.github.scheduler.utils;

import com.github.scheduler.model.JobResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class WrappedRunnable implements Runnable{
    private static final Logger LOG = LogManager.getLogger(WrappedRunnable.class);
    private JobResponse response;

    public JobResponse getResponse(){
        return this.response;
    }

    @Override
    public void run() {
        try{
            this.response = doWork();
        }catch (Exception e){
            String errorMsg = Utils.stackTrace(e);
            LOG.error(errorMsg);
        }

    }

    abstract protected JobResponse doWork() throws Exception;
}
