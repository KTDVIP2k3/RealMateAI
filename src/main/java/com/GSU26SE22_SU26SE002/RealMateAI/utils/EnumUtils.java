package com.GSU26SE22_SU26SE002.RealMateAI.utils;

public class EnumUtils {
    public static <T extends Enum<T>> T fromString(Class<T> enumClass, String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String upperValue = value.trim().toUpperCase();
        for (T constant : enumClass.getEnumConstants()) {
            if (constant.name().toUpperCase().equals(upperValue)) {
                return constant;
            }
        }
        return null;
    }
}
