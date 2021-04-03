package com.github.scheduler.model;

public class JobResponse {
    private final String jobId;
    private int retCode;
    private String output;
    private String error;

    public JobResponse(String jobId){
        this.jobId = jobId;
    }

    public void setRetCode(int retCode){
        this.retCode = retCode;
    }

    public int getRetCode(){
        return this.retCode;
    }

    public void setOutput(String output){
        this.output = output;
    }

    public String getOutput(){
        return this.output;
    }

    public void setError(String error){
        this.error = error;
    }

    public String getError(){
        return this.error;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id:");
        sb.append(this.jobId);
        sb.append("\n");
        sb.append("code:");
        sb.append(retCode);
        sb.append("\n");
        sb.append("output:");
        sb.append(this.output);
        sb.append("\n");
        sb.append("error:");
        sb.append(this.error);
        sb.append("\n");
        return sb.toString();
    }
}
