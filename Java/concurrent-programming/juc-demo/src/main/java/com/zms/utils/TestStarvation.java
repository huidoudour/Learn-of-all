package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 *饥饿现象
 * 程序流程：
 * 1.主线程向线程池提交两个 "处理点餐" 任务
 * 2.线程池线程在碰到pool.submit后调到到try块中的f.get()，这时两个线程都会阻塞
 */
@Slf4j
public class TestStarvation {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = pool.submit(() -> {
                log.debug("做菜");
                return "宫保鸡丁";
            });
            try {
                log.debug("上菜：{}", f.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });


        pool.execute(() -> {
            log.debug("处理点餐...");
            Future<String> f = pool.submit(() -> {
                log.debug("做菜");
                return "番茄炒鸡蛋";
            });
            try {
                log.debug("上菜：{}", f.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
