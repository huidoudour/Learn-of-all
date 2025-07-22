package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TestScheduledThreadPoolExecutor {
    public static void main(String[] args) {

        method1();
    }

    public static void method1() {
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

        pool.schedule(() -> {
            log.debug("running1...");
        }, 1000, TimeUnit.MILLISECONDS);

        pool.schedule(() -> {
            log.debug("running2...");
        }, 1000, TimeUnit.MILLISECONDS);
    }
}
