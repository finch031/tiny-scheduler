package com.github.scheduler.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public final class Utils {
    public static final boolean WINDOWS = System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Windows");
    public static final boolean LINUX = System.getProperty("os.name") != null && System.getProperty("os.name").startsWith("Linux");

    public static final Pattern COMMON_DATE_TIME_PATTERN = Pattern.compile(
            //year    month     day        hour       minute     second       millis  time zone
            "[0-9]{4}-[01][0-9]-[0-3][0-9]T[0-2][0-9]:[0-5][0-9]:[0-5][0-9]\\.[0-9]{3}(Z|[+\\-][0-9]{2}(:[0-9]{2}))"
    );

    public static final Pattern DATE_PATTERN = Pattern.compile(
            // year    month     day
            "[0-9]{4}-[01][0-9]-[0-3][0-9]"
    );

    public static final Pattern TIME_PATTERN = Pattern.compile(
            // hour       minute     second
            "[0-2][0-9]:[0-5][0-9]:[0-5][0-9]"
    );

    private static final String[] WEEK_DAYS = {"sunday","monday","tuesday","wednesday","thursday","friday","saturday"};

    // no instance.
    private Utils(){}

    /**
     * 查找指定命令行参数的索引位置.
     * @param args 命令行参数数组.
     * @param param 待查找的命令行参数.
     * @return index 参数索引位置,-1表示没有查找到.
     * */
    public static int paramIndexSearch(String[] args,String param){
        int index = -1;
        for (int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase(param)){
                index = i;
                break;
            }
        }

        return index;
    }

    /**
     * 简单日期格式匹配
     * @param format 日期格式
     * @param dateStr 日期值
     * */
    public static boolean simpleDateFormatMatch(String format,String dateStr){
        DateFormat dateFormat = new SimpleDateFormat(format);
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 正则表达式日期匹配
     * @param dateStr 日期值
     * */
    public static boolean regexDateFormatMatch(String dateStr){
        return DATE_PATTERN.matcher(dateStr).matches();
    }

    /**
     * 正则表达式时间匹配
     * @param timeStr 日期值
     * */
    public static boolean regexTimeFormatMatch(String timeStr){
        return TIME_PATTERN.matcher(timeStr).matches();
    }

    /**
     * date parse.
     * @param format date format.
     * @param dateStr date string.
     * */
    public static Date dateParse(String format,String dateStr){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date date = null;
        try{
            date = sdf.parse(dateStr);
        }catch (ParseException pe){
            pe.printStackTrace();
        }
        return date;
    }

    /**
     * 时间戳转日期时间
     * @param timeStamp 时间戳
     * @param pattern 日期时间格式
     * */
    public static String timeStampToDateTime(long timeStamp,String pattern){
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        Date date = new Date(timeStamp);
        return sdf.format(date);
    }

    /**
     * 获取每日开始时间戳
     * */
    public static long dailyStartTimeStamp(){
        // 获取当前日期
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取下一天的开始时间戳
     * */
    public static long nextDayStartTimeStamp(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY,24);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        return calendar.getTimeInMillis();
    }

    /**
     * 获取指定时间在一天中的第N秒.
     * @param timeStr 时间字符串,如: 01:15:10
     * */
    public static int theSecondOfDay(String timeStr){
        LocalTime localTime = LocalTime.parse(timeStr);
        return localTime.toSecondOfDay();
    }

    /**
     * 获取当天是星期几
     * */
    public static String currentDayInWeek(){
        Calendar calendar = Calendar.getInstance();
        int dayInWeekCode = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if(dayInWeekCode < 0){
            dayInWeekCode = 0;
        }
        return WEEK_DAYS[dayInWeekCode];
    }

    /**
     * 当前日期
     * */
    public static String currentDate(){
        return timeStampToDateTime(System.currentTimeMillis(),"yyyy-MM-dd");
    }

    /**
     * 获取下一次执行是星期几
     * @param dayInWeeksList 任务设定的执行日列表.
     * */
    public static Tuple<String,Long> getNextExecuteDayInWeek(List<String> dayInWeeksList){
        Calendar calendar = Calendar.getInstance();
        int dayInWeekCode = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if(dayInWeekCode < 0){
            dayInWeekCode = 0;
        }

        String nextDayInWeek = null;

        int daysDelta = 0;
        // 当周
        for(int i = dayInWeekCode + 1; i < WEEK_DAYS.length; i++){
            daysDelta++;
            if(dayInWeeksList.contains(WEEK_DAYS[i])){
                nextDayInWeek = WEEK_DAYS[i];
                break;
            }
        }

        // 下周
        if(nextDayInWeek == null){
            for(int i = 0; i < dayInWeekCode; i++){
                daysDelta++;
                if(dayInWeeksList.contains(WEEK_DAYS[i])){
                    nextDayInWeek = WEEK_DAYS[i];
                    break;
                }
            }
        }

        long millisDelta = daysDelta * 86400000L;

        return Tuple.tuple(nextDayInWeek,millisDelta);
    }

    /**
     * 获取下一次执行的日期
     * @param datesList 任务执行日期列表.
     * */
    public static String getNextExecuteDate(List<String> datesList){
        String currentDate = currentDate();
        datesList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });

        String nextExecuteDate = null;
        for (String date : datesList) {
            if(date.compareTo(currentDate) > 0){
                nextExecuteDate = date;
                break;
            }
        }

        return nextExecuteDate;
    }

    /**
     * convert List<String> to String.
     * */
    public static String datesListAsString(List<String> datesList){
        StringBuilder sb = new StringBuilder();
        for (String s : datesList) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Given a time expressed in milliseconds, append the time formatted as
     * "hh[:mm[:ss]]".
     *
     * @param buf    Buffer to append to
     * @param millis Milliseconds
     */
    public static void appendPosixTime(StringBuilder buf, int millis) {
        if (millis < 0) {
            buf.append('-');
            millis = -millis;
        }
        int hours = millis / 3600000;
        buf.append(hours);
        millis -= hours * 3600000;
        if (millis == 0) {
            return;
        }
        buf.append(':');
        int minutes = millis / 60000;
        if (minutes < 10) {
            buf.append('0');
        }
        buf.append(minutes);
        millis -= minutes * 60000;
        if (millis == 0) {
            return;
        }
        buf.append(':');
        int seconds = millis / 1000;
        if (seconds < 10) {
            buf.append('0');
        }
        buf.append(seconds);
    }


    /**
     * Ensures that an object reference passed as a parameter to the calling method is not null.
     *
     * @param reference an object reference
     * @return the non-null reference that was validated
     * @throws NullPointerException if {@code reference} is null
     */
    public static <T extends  Object> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    /**
     * List转数组.
     * */
    public static String[] cmdListToArray(List<String> cmdList){
        String[] array = new String[cmdList.size()];
        for(int i = 0; i < cmdList.size(); i++){
            array[i] = cmdList.get(i);
        }

        return array;
    }

    /**
     * create job id.
     * @param scheduleMode schedule mode.
     * @param tid thread id.
     * */
    public static String createJobId(int scheduleMode,long tid){
        String now = timeStampToDateTime(System.currentTimeMillis(),"yyyyMMddHHmmss");
        // schedule mode + thread id + current datetime
        return scheduleMode + "_" + tid + "_" + now;
    }

    /**
     * sleep quietly!
     * @param millis millis.
     * */
    public static void sleepQuietly(long millis){
        try{
            Thread.sleep(millis);
        }catch (InterruptedException ie){
            // ignore.
        }
    }

}
