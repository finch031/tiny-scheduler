package com.github.scheduler.utils;

import com.github.scheduler.model.JobResponse;

public abstract class WrappedRunnable implements Runnable{
    private JobResponse response;

    public JobResponse getResponse(){
        return this.response;
    }

    @Override
    public void run() {
        try{
            this.response = doWork();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    abstract protected JobResponse doWork() throws Exception;
}
