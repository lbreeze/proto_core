package ru.v6.mark.prototype.service.util;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtil {

    public static final String PRODUCED_DATE_FORMAT = "yyyy-MM-dd";

    public static Date plusDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return cutTime(calendar.getTime());
    }

    public static Date plusMinute(Date date, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minute);
        return calendar.getTime();
    }

    public static Date plusSecond(Date date, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.SECOND, second);
        return calendar.getTime();
    }

    public static Date plusHour(Date date, int hour) {
        return plusMinute(date, hour * 60);
    }

    public static Date cutTime(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar.getTime();
    }

    public static Date parseDate(final String dateStr, DateFormat df) {
        try {
            return df.parse(dateStr);
        } catch (Exception e) {
            e.printStackTrace();
            return new Date();
        }
    }

    public static Date setDate(int y, int m, int d) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(y, m, d, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static String getStringByFormat(Date dt, DateFormat dateFormat) {
        return dateFormat.format(dt);
    }
}
