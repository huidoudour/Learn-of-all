package com.zms.base;

import lombok.extern.slf4j.Slf4j;

// 使用wait、notify实现先打印2，后打印1
@Slf4j
public class TestRunOrder1 {
    private static Object lock = new Object();
    private static volatile boolean t2runned = false;

    public static void main(String[] args) {
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                log.info("2");
                t2runned = true;
                lock.notify();
            }
        }, "t2");

        Thread t1 = new Thread(() -> {
            synchronized (lock) {
                while(!t2runned) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("1");
                }
            }
        }, "t1");

        t1.start();
        t2.start();
    }
}
