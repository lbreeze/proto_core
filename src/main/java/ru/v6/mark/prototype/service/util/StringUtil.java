package ru.v6.mark.prototype.service.util;

import java.util.Map;

public class StringUtil {

    public static boolean hasLength(String str) {
        return hasLength((CharSequence) str);
    }

    private static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    public static String getValue(Map<String, String> result, String key) {
        if (result.get(key) != null && hasLength(String.valueOf(result.get(key)))) {
            return String.valueOf(result.get(key));
        }
        return "";
    }
}
