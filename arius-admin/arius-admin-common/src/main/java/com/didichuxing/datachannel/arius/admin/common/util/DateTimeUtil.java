package com.didichuxing.datachannel.arius.admin.common.util;

/**
 * @author: D10865
 * @description:
 * @date: Create on 2019/1/21 下午5:09
 * @modified By D10865
 */

import com.didichuxing.datachannel.arius.admin.common.Tuple;
import com.didichuxing.tunnel.util.log.ILog;
import com.didichuxing.tunnel.util.log.LogFactory;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: D10865
 * @Description:
 * @Date: Create on 2018/8/29 上午10:37
 * @Modified By
 */
public class DateTimeUtil {

    private static final ILog LOGGER = LogFactory.getLog(DateTimeUtil.class);

    // 不同日期格式的正则匹配
    private static Map<String, Pattern> dateFormatPattern = Maps.newHashMap();

    static {
        dateFormatPattern.put("_YYYY-MM", Pattern.compile("\\d{4}-\\d{2}"));
        dateFormatPattern.put("_YYYYMM", Pattern.compile("\\d{6}"));
        dateFormatPattern.put("_yyyyMM", Pattern.compile("\\d{6}"));

        dateFormatPattern.put("YYYYMMdd", Pattern.compile("\\d{8}"));
        dateFormatPattern.put("_YYYYMMdd", Pattern.compile("\\d{8}"));
        dateFormatPattern.put("_YYYY-MM-dd", Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        dateFormatPattern.put("__YYYY-MM-dd", Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        dateFormatPattern.put("_yyyyMMdd", Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));
        dateFormatPattern.put("_yyyy-MM-dd", Pattern.compile("\\d{4}-\\d{2}-\\d{2}"));

        // 除了定义的可以还有
        dateFormatPattern.put("YYYY_MM_dd", Pattern.compile("\\d{4}_\\d{2}_\\d{2}"));
    }

    public static String getDateStr(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(time));
    }

    public static String getDateStr(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    public static Long getTime(String date) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.isBlank(date)) {
            date = dateTimeFormatter.format(ZonedDateTime.now().minus(1, ChronoUnit.DAYS));
        }
        LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDate.atStartOfDay().atZone(zone).toInstant();
        return instant.toEpochMilli();
    }

    public static Date getBeforeDays(Date time, int before) {
        if (time == null){return null;}
        if (before < 1){return null;}

        Calendar c = Calendar.getInstance();
        c.setTime(time);
        c.set(Calendar.DATE, c.get(Calendar.DATE) - before);

        return c.getTime();
    }

    /**
     * 获取索引日期
     *
     * @param date
     * @return
     */
    public static String getIndexDate(String indexName, String date, String dateFormat) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        if (StringUtils.isBlank(dateFormat)) {
            return null;
        }

        String indexDate = date;
        // 月日方式, 添加年份
        if ("MMdd".equals(dateFormat)) {
            indexDate = String.valueOf(LocalDate.now().getYear()).concat(date);
            dateFormat = "YYYYMMdd";
        }
        // 去掉下划线
        if (dateFormat.contains("_")) {
            indexDate = date.substring(1);
        }

        // 使用正则提取时间
        Pattern pattern = dateFormatPattern.get(dateFormat);
        if (pattern == null) {
            LOGGER.error("{} can't find pattern dateFormat {}, date {}", indexName, dateFormat, date);
            return indexDate;
        }

        try {
            Matcher matcher = pattern.matcher(indexDate);
            if (matcher.find()) {
                return matcher.group();
            }

            // 这里可能由于之前时间格式做过修改，历史索引名称和目前时间格式不匹配
            for (Pattern p : dateFormatPattern.values()) {
                matcher = p.matcher(indexDate);

                if (matcher.find()) {
                    return matcher.group();
                }
            }
            LOGGER.error("{} dateFormat {} not match date {}", indexName, dateFormat, date);

        } catch (Exception e) {
            LOGGER.error("fail to getIndexDate {}, {}, {}", indexName, dateFormat, date, e);
        }

        return indexDate;
    }

    /**
     * 是否在指定时间之前
     *
     * @param formatDateTime
     * @param dayOffset
     * @return
     */
    public static boolean isBeforeDateTime(String formatDateTime, int dayOffset) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z");
        ZonedDateTime markDateTime = ZonedDateTime.parse(formatDateTime, dateTimeFormatter);
        ZonedDateTime expectDateTime = ZonedDateTime.now().minus(dayOffset, ChronoUnit.DAYS);

        return markDateTime.isBefore(expectDateTime);
    }

    public static String getDateTimeStr(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(time));
    }

    /**
     * 是否在指定时间之前
     *
     * @param date
     * @param dayOffset
     * @return
     */
    public static boolean isBeforeDateTime(Date date, int dayOffset) {
        ZonedDateTime markDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        ZonedDateTime expectDateTime = ZonedDateTime.now().minus(dayOffset, ChronoUnit.DAYS);

        return markDateTime.isBefore(expectDateTime);
    }

    /**
     * 是否在指定时间之后
     *
     * @param date
     * @param dayOffset
     * @return
     */
    public static boolean isAfterDateTime(Date date, int dayOffset) {
        ZonedDateTime markDateTime = ZonedDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        ZonedDateTime expectDateTime = ZonedDateTime.now().minus(dayOffset, ChronoUnit.DAYS);

        return markDateTime.isAfter(expectDateTime);
    }

    /**
     * 获取当前格式化时间
     *
     * @return
     */
    public static String getCurrentFormatDateTime() {
        String formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z").format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.systemDefault()));

        return formatDateTime;
    }

    /**
     * 格式化时间
     *
     * @param timestamp
     * @return
     */
    public static String formatTimestamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z").format(zonedDateTime);
    }

    /**
     * 格式化时间
     *
     * @param timestamp
     * @return
     */
    public static String formatTimestamp2(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").format(zonedDateTime);
    }

    /**
     * 获取指定偏移日期的格式化结果
     *
     * @param offset
     * @return
     */
    public static String getFormatDayByOffset(int offset) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dateTimeFormatter.format(ZonedDateTime.now().minus(offset, ChronoUnit.DAYS));
    }

    public static String getFormatMonthByOffset(int offset) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return dateTimeFormatter.format(ZonedDateTime.now().minus(offset, ChronoUnit.DAYS));
    }

    /**
     * 获取格式化的昨天
     *
     * @param dateFormat
     * @return
     */
    public static String getYesterdayDayByFormat(String dateFormat) {
        try {
            LocalDateTime dateTime = LocalDateTime.now().minusDays(1);
            // 根据时间格式转换为格式化后的时间字符串
            return DateTimeFormatter.ofPattern(dateFormat.replaceAll("Y", "y")).format(dateTime);
        } catch (Exception e) {
            LOGGER.error("class=DateTimeUtil||method=getYesterdayDayByFormat||errMsg=format date error {}. ",
                    dateFormat, e);
            return null;
        }
    }

    /**
     * 获取格式化的上个月
     *
     * @param dateFormat
     * @return
     */
    public static String getLastMonthDayByFormat(String dateFormat) {
        try {
            LocalDateTime dateTime = LocalDateTime.now().minusMonths(1);
            // 根据时间格式转换为格式化后的时间字符串
            return DateTimeFormatter.ofPattern(dateFormat.replaceAll("Y", "y")).format(dateTime);
        } catch (Exception e) {
            LOGGER.error("class=DateTimeUtil||method=getLastMonthDayByFormat||errMsg=format date error {}. ",
                    dateFormat, e);
            return null;
        }
    }

    /**
     * 获取格式化的去年
     *
     * @param dateFormat
     * @return
     */
    public static String getLastYearDayByFormat(String dateFormat) {
        try {
            LocalDateTime dateTime = LocalDateTime.now().minusYears(1);
            // 根据时间格式转换为格式化后的时间字符串
            return DateTimeFormatter.ofPattern(dateFormat.replaceAll("Y", "y")).format(dateTime);
        } catch (Exception e) {
            LOGGER.error("class=DateTimeUtil||method=getLastMonthDayByFormat||errMsg=format date error {}. ",
                    dateFormat, e);
            return null;
        }
    }


    /**
     * 获取周几
     *
     * @param dateTime
     * @return
     */
    public static Integer getDayOfWeek(LocalDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue();
    }

    public static Integer getDayOfCurrentMonth(){
        Calendar now = Calendar.getInstance();

        return now.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 根据时间获取起始结束时间
     *
     * @param date
     * @return
     */
    public static Tuple<String, String> getStartEndTimeByDate(String date) {
        //计算时间范围
        String startDate = "", endDate = "";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.isBlank(date)) {
            date = dateTimeFormatter.format(ZonedDateTime.now().minus(1, ChronoUnit.DAYS));
        }
        LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
        ZonedDateTime dateTime = ZonedDateTime.of(localDate, LocalDateTime.now().toLocalTime(), ZoneId.systemDefault());
        endDate = dateTimeFormatter.format(dateTime.plus(1, ChronoUnit.DAYS));
        startDate = dateTimeFormatter.format(dateTime);

        return new Tuple<>(startDate, endDate);
    }

    /**
     * 根据时间获取起始结束时间
     *
     * @param date
     * @return
     */
    public static Tuple<String, String> getStartEndWeekTimeByDate(String date) {
        String startDate = "", endDate = "";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (StringUtils.isBlank(date)) {
            date = dateTimeFormatter.format(ZonedDateTime.now());
        }
        LocalDate localDate = LocalDate.parse(date, dateTimeFormatter);
        ZonedDateTime dateTime = ZonedDateTime.of(localDate, LocalDateTime.now().toLocalTime(), ZoneId.systemDefault());
        endDate = dateTimeFormatter.format(dateTime.plus(1, ChronoUnit.DAYS));
        startDate = dateTimeFormatter.format(dateTime.minus(7, ChronoUnit.DAYS));

        return new Tuple<>(startDate, endDate);
    }

    /**
     * 获取时间范围
     *
     * @param ariusCreateTime
     * @return
     */
    public static Tuple<Long, Long> getAriusCreateTimeRange(String ariusCreateTime) {
        long startTimeMilli = System.currentTimeMillis() - 43200000;
        long endTimeMilli = System.currentTimeMillis() + 43200000;
        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(ariusCreateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z"));
            ZonedDateTime startDate = dateTime.minus(12, ChronoUnit.HOURS);
            ZonedDateTime endDate = dateTime.plus(12, ChronoUnit.HOURS);
            startTimeMilli = startDate.toInstant().toEpochMilli();
            endTimeMilli = endDate.toInstant().toEpochMilli();
        } catch (Exception e) {
            LOGGER.error("fail to parse data {}", ariusCreateTime, e);
        }

        return new Tuple<>(startTimeMilli, endTimeMilli);
    }

    /**
     * 得到简化的时间格式
     *
     * @param date 例如2018-10-08 16:00:00.330 +0800
     * @return
     */
    public static String getShortDay(String date) {
        int index = date.indexOf(" ");
        if (index > 0) {
            return date.substring(0, index);
        }
        return date;
    }

    /**
     * 获取某天凌晨的时间
     * @param time
     * @return
     */
    public static Date getZeroDate(Date time, int offset) {
        if (time == null){time = new Date();}

        Calendar c = Calendar.getInstance();
        c.setTime(time);
        c.set(Calendar.DATE, c.get(Calendar.DATE) - offset);

        Date offsetDate = c.getTime();

        offsetDate = DateUtils.setHours(offsetDate, 0);
        offsetDate = DateUtils.setMinutes(offsetDate, 0);
        offsetDate = DateUtils.setSeconds(offsetDate, 0);
        offsetDate = DateUtils.setMilliseconds(offsetDate, 0);
        return offsetDate;
    }

    public static long getCurrentMonthEnd(){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.MONTH, 0);

        int currentMonthMaxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), currentMonthMaxDay, 23, 59, 59);
        return c.getTimeInMillis();
    }

    public static long getCurrentMonthStart(){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.MONTH, 0);
        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1, 00, 00, 00);
        return c.getTimeInMillis();
    }

    public static long getLastMonthEnd(){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.MONTH, -1);

        int lastMonthMaxDay = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), lastMonthMaxDay, 23, 59, 59);
        return c.getTimeInMillis();
    }

    public static long getLastMonthStart(){
        Calendar c=Calendar.getInstance();
        c.add(Calendar.MONTH, -1);
        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1, 00, 00, 00);
        return c.getTimeInMillis();
    }

    /**
     * 获取到当前时间戳，按分钟对齐
     * @return
     */
    public static long getCurrentTimestampMinute() {
        long timeStamp = System.currentTimeMillis() / 1000;
        long seconds = timeStamp % 60;
        if (seconds >= 30) {
            timeStamp = timeStamp - seconds + 60;
        } else {
            timeStamp = timeStamp - seconds;
        }

        return timeStamp;
    }

}
