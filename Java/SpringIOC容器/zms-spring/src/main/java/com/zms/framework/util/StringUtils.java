package com.zms.framework.util;

/**
 * @version v1.0
 * @ClassName: StringUtils
 * @Description: TODO
 * @Author: zms
 */
public class StringUtils {
    public StringUtils() {
    }

    public static String getSetterMethodFieldName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

    }
}
