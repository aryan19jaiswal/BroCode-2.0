package com.broCode.util;

public class StringUtils {
    public static String getOrDefault(String str, String defaultValue) {
        return (str != null) ? str : defaultValue;
    }
}
