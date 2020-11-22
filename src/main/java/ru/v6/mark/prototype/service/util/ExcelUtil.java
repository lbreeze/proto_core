package ru.v6.mark.prototype.service.util;

public class ExcelUtil {

    public static int colNameToIndex(String name) {
        int result = 0;
        for (int i = 0; i < name.length(); i++) {
            result *= 26;
            result += name.charAt(i) - 64;
        }
        return result - 1;
    }

    public static String colIndexToName(int index) {
        StringBuilder result = new StringBuilder();
        while (index  > 0) {
            char c = (char) (index % 26 + 65);
            result.insert(0, c);
            index -= index % 26 + 1;
            index /= 26;
        }
        return result.toString();
    }
}
