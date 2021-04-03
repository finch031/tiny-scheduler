package com.github.scheduler;

import com.github.scheduler.model.JobResponse;
import com.github.scheduler.model.JobResponseHandler;
import com.github.scheduler.runner.DailyFixTimeJobRunner;
import com.github.scheduler.runner.FixRateJobRunner;
import com.github.scheduler.runner.JobRunner;
import com.github.scheduler.runner.OnceJobRunner;
import com.github.scheduler.utils.ScheduleMode;
import com.github.scheduler.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.scheduler.utils.Utils.paramIndexSearch;

/**
 * @author yusheng
 * @version 0.0.1
 * @datetime 2021-03-28 20:40
 * @description a pure tiny cmd based scheduler.
 *
 *   基本调度模式：
 *   1. 单次执行.
 *   2. 每隔指定时间执行一次.
 *   3. 每日指定时间点执行一次.
 *   4. 每个星期x1、星期x2、星期x3等的指定时间点执行一次.
 *   5. 指定的日期1、日期2、日期3等的指定时间点执行一次.
 *
 * */
public class TinyScheduler {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static final String USAGE =
            "usage: " + LINE_SEPARATOR +
            " java -jar tiny-scheduler-0.0.1.jar " + LINE_SEPARATOR +
                    "    ---schedule_mode 1|2|3|4|5 " + LINE_SEPARATOR +
                    "    ---schedule_cmd [args...] " + LINE_SEPARATOR +
                    "    ---delay delay" + LINE_SEPARATOR +
                    "    ---period period " + LINE_SEPARATOR +
                    "    ---timeunit milliseconds|seconds|minutes|hours|days " + LINE_SEPARATOR +
                    "    ---day_in_weeks monday|tuesday|wednesday|thursday|friday|saturday|sunday(comma-delimited) " + LINE_SEPARATOR +
                    "    ---dates dates(date format must be:yyyy-MM-dd,comma-delimited) " + LINE_SEPARATOR +
                    "    ---execute_time execute_time(time format must be:HH:mm:ss)"
            ;

    private static void printUsageAndExit(String...messages){
        for (String message : messages) {
            System.err.println(message);
        }
        System.err.println(USAGE);
        System.exit(1);
    }

    private static long getDelayParam(String[] args){
        long delay = 0L;
        int index = paramIndexSearch(args,"---delay");
        if(index != -1){
            String delayStr = args[index+1];
            delay = Long.parseLong(delayStr);
        }else {
            printUsageAndExit("error: ---delay not found!");
        }

        return delay;
    }

    private static long getPeriodParam(String[] args){
        long period = 0L;
        int index = paramIndexSearch(args,"---period");
        if(index != -1){
            String periodStr = args[index+1];
            period = Long.parseLong(periodStr);
        }else {
            printUsageAndExit("error: ---period not found!");
        }
        return period;
    }

    private static TimeUnit getTimeUnitParam(String[] args){
        TimeUnit timeUnit = null;
        int index = paramIndexSearch(args,"---timeunit");
        if(index != -1){
            String timeUnitStr = args[index+1];
            switch (timeUnitStr){
                case "milliseconds" :
                    timeUnit = TimeUnit.MILLISECONDS;
                    break;
                case "seconds":
                    timeUnit = TimeUnit.SECONDS;
                    break;
                case "minutes":
                    timeUnit = TimeUnit.MINUTES;
                    break;
                case "hours":
                    timeUnit = TimeUnit.HOURS;
                    break;
                case "days":
                    timeUnit = TimeUnit.DAYS;
                    break;
            }

            if(timeUnit == null){
                printUsageAndExit("error: timeunit is invalid!");
            }
        }else {
            printUsageAndExit("error: ---timeunit not found!");
        }
        return timeUnit;
    }

    private static List<String> getDayInWeeksParam(String[] args){
        List<String> dayInWeeksList = new ArrayList<>();
        int index = paramIndexSearch(args,"---day_in_weeks");
        if(index != -1){
            String tempStr = args[index+1];
            String[] tempArr = tempStr.split(",");
            for (String s : tempArr) {
                if(s.equalsIgnoreCase("monday") ||
                   s.equalsIgnoreCase("tuesday") ||
                   s.equalsIgnoreCase("wednesday") ||
                   s.equalsIgnoreCase("thursday") ||
                   s.equalsIgnoreCase("friday") ||
                   s.equalsIgnoreCase("saturday") ||
                   s.equalsIgnoreCase("sunday")){
                    dayInWeeksList.add(s);
                }else{
                    printUsageAndExit("error: day_in_week is invalid:" + s);
                }
            }

            if(dayInWeeksList.isEmpty()){
                printUsageAndExit("error: ---day_in_weeks is empty!");
            }
        }else{
            printUsageAndExit("error: ---day_in_weeks not found!");
        }

        return dayInWeeksList;
    }

    private static List<Date> getDatesParam(String[] args){
        List<Date> datesList = new ArrayList<>();
        int index = paramIndexSearch(args,"---dates");
        if(index != -1){
            String tempStr = args[index+1];
            String[] tempArr = tempStr.split(",");
            for (String s : tempArr) {
                boolean match = Utils.regexDateFormatMatch(s);
                if(match){
                    Date date = Utils.dateParse("yyyy-MM-dd",s);
                    if(date != null){
                        datesList.add(date);
                    }else{
                        printUsageAndExit("error: date is invalid: " + s);
                    }
                }else{
                    printUsageAndExit("error: date is invalid: " + s);
                }
            }
            printUsageAndExit("error: ---dates is empty!");
        }else{
            printUsageAndExit("error: ---dates not found!");
        }

        return datesList;
    }

    private static String getExecuteTimeParam(String[] args){
        String executeTime = null;
        int index = paramIndexSearch(args,"---execute_time");
        if(index != -1){
            String executeTimeTemp = args[index+1];
            boolean match = Utils.regexTimeFormatMatch(executeTimeTemp);
            if(match){
                executeTime = executeTimeTemp;
            }else{
                printUsageAndExit("error: execute_time is invalid:" + executeTimeTemp);
            }
        }else{
            printUsageAndExit("error: ---execute_time not found!");
        }

        return executeTime;
    }

    /**
     * 命令行解析并调度执行.
     * @param args 命令行参数
     * */
    private static JobRunner cmdParser(String[] args){
        int scheduleModeCode = -1;
        int scheduleModeIndex = paramIndexSearch(args,"---schedule_mode");
        if(scheduleModeIndex != -1){
            String scheduleModeStr = args[scheduleModeIndex+1];
            if(scheduleModeStr.equals("1") || scheduleModeStr.equals("2") ||
               scheduleModeStr.equals("3") || scheduleModeStr.equals("4")){
                scheduleModeCode = Integer.parseInt(scheduleModeStr);
            }else {
                printUsageAndExit("error: schedule_mode is invalid!");
            }
        }else{
            printUsageAndExit("error: ---schedule_mode not found!");
        }

        List<String> cmdList = new ArrayList<>();
        int scheduleCmdIndex = paramIndexSearch(args,"---schedule_cmd");
        if(scheduleCmdIndex != -1){
            for(int i = scheduleCmdIndex + 1; i < args.length; i++){
                String arg = args[i];
                if(arg.startsWith("---")){
                    break;
                }
                cmdList.add(arg);
            }
        }else{
         printUsageAndExit("error: ---schedule_cmd not found!");
        }

        if(cmdList.isEmpty()){
            printUsageAndExit("error: ---schedule_cmd is empty!");
        }

        // 运行任务
        JobRunner jobRunner = null;

        ScheduleMode scheduleMode;
        switch (scheduleModeCode){
            case 1 :
                scheduleMode = ScheduleMode.ONCE;
                long delay = getDelayParam(args);
                TimeUnit onceJobTimeUnit = getTimeUnitParam(args);
                jobRunner = new OnceJobRunner(scheduleMode,cmdList,delay,onceJobTimeUnit);
                break;
            case 2 :
                scheduleMode = ScheduleMode.AT_FIXED_RATE;
                long initialDelay = getDelayParam(args);
                long period = getPeriodParam(args);
                TimeUnit timeUnit = getTimeUnitParam(args);
                jobRunner = new FixRateJobRunner(scheduleMode,cmdList,initialDelay,period,timeUnit);
                break;
            case 3 :
                scheduleMode = ScheduleMode.DAILY_FIXED_TIME;
                String executeTime = getExecuteTimeParam(args);
                jobRunner = new DailyFixTimeJobRunner(scheduleMode,cmdList,executeTime);
                break;
            case 4:
                scheduleMode = ScheduleMode.DAY_IN_WEEK_FIXED_TIME;
                List<String> dayInWeeksList = getDayInWeeksParam(args);
                String executeTime2 = getExecuteTimeParam(args);

                break;
            case 5:
                scheduleMode = ScheduleMode.DATE_FIXED_TIME;
                List<Date> datesList = getDatesParam(args);
                String executeTime3 = getExecuteTimeParam(args);

                break;
            default:
                scheduleMode = null;
        }


        return jobRunner;
    }


    public static void main(String[] args){
        if(args.length < 1){
            printUsageAndExit("error: args length is less than 1!");
        }

        JobRunner jobRunner = cmdParser(args);

        System.out.println(jobRunner.printCmdList());

        jobRunner.setResponseHandler(new JobResponseHandler() {
            @Override
            public void handler(JobResponse jobResponse) {
                System.out.println(jobResponse);
            }
        });

        jobRunner.start();

        jobRunner.waitComplete();

        // jobRunner.stop();

    }

}
