package com.zms.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TestSimpleFormatDate {
    public static void main(String[] args) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < 10; i++) {
                new Thread(() -> {
                    synchronized (TestSimpleFormatDate.class) {
                    try {
                        Date date = sdf.parse("2025-07-19");
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    }
                }).start();
        }
    }
}
