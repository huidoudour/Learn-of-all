package com.zms.base;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestLiveLock {
    private static volatile int counter = 10;
    public static void main(String[] args) {
        new Thread(() -> {
            while(counter < 20) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                counter++;
                log.debug("线程：{}, counter = {}",Thread.currentThread().getId(), counter);
            }
        }, "t1").start();

        new Thread(() -> {
            while(counter > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                counter--;
                log.debug("线程：{}, counter = {}",Thread.currentThread().getId(), counter);
            }
        }, "t2").start();
    }
}
