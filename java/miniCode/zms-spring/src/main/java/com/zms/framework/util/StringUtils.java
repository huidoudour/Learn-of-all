package com.zms.framework.util;

/**
 * @version v1.0
 * @ClassName: StringUtils
 * @Description: 字符串工具类
 * @Author: zms
 */
public class StringUtils {
    public StringUtils() {
    }

    public static String getSetterMethodFieldName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

    }
}
