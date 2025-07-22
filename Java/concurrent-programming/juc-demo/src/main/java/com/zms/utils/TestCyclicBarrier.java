package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class TestCyclicBarrier {
    public static void main(String[] args) {
        // 创建固定大小为 2 的线程池，确保每次最多两个线程并行执行
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 创建 CyclicBarrier，指定需要 2 个线程到达屏障点
        // 当条件满足时，会先执行传入的 Runnable（屏障动作）
        CyclicBarrier cyclicBarrier = new CyclicBarrier(2, () -> {
            log.debug("task1 task2 finished"); // 所有线程到达屏障点后执行
        });

        // 循环执行 3 轮任务
        for(int i = 0; i < 3; i++) {
            // 提交第一个任务
            executorService.execute(() -> {
                log.debug("task1 start");
                try {
                    Thread.sleep(1000); // 模拟耗时操作
                    log.debug("task1 end");
                    cyclicBarrier.await(); // 到达屏障点，等待其他线程
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            });

            // 提交第二个任务
            executorService.execute(() -> {
                log.debug("task2 start");
                try {
                    Thread.sleep(1000); // 模拟耗时操作
                    log.debug("task2 end");
                    cyclicBarrier.await(); // 到达屏障点，等待其他线程
                } catch (InterruptedException | BrokenBarrierException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // 关闭线程池（注意：这里不会等待任务完成，仅拒绝新任务）
        // 若需要确保所有任务完成后关闭，应使用 awaitTermination
        executorService.shutdown();
    }
}
