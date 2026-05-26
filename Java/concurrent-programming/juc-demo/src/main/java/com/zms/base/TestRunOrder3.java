package com.zms.base;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

// 使用wait、notify实现先打印2，后打印1
@Slf4j
public class TestRunOrder3 {
    private static ReentrantLock lock = new ReentrantLock();
    private static boolean t2isRunned = false;
    private static Condition condition = lock.newCondition();

    public static void main(String[] args) {
        Thread t2 = new Thread(() -> {
            lock.lock();
            try {
                log.info("2");
                condition.signal();
            } finally {
                lock.unlock();
            }
        }, "t2");

        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                while (!t2isRunned) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("1");
                }
            } finally {
                lock.unlock();
            }
        }, "t1");

        t1.start();
        t2.start();
    }
}
