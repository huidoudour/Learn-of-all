package com.zms.utils;

import java.util.concurrent.Semaphore;

public class SemaphoreExample {
    private static final Semaphore SEMAPHORE = new Semaphore(3); // 允许3个线程同时访问

    public static void main(String[] args) {
        // 创建10个线程，但最多3个线程可同时执行任务
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    SEMAPHORE.acquire(); // 获取许可证
                    System.out.println(Thread.currentThread().getName() + " 获取到许可证，开始执行");
                    Thread.sleep(2000); // 模拟业务操作
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    SEMAPHORE.release(); // 释放许可证
                    System.out.println(Thread.currentThread().getName() + " 释放许可证");
                }
            }, "Thread-" + i).start();
        }
    }
}
