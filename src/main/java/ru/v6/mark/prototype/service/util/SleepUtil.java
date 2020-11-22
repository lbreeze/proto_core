package ru.v6.mark.prototype.service.util;

import java.util.concurrent.TimeUnit;

public class SleepUtil {

    public static final int CRPT_QUERY_INTERVAL = 2000;

    public static void sleep(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
