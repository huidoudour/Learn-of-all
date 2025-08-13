package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
public class TestJDKThreadPool {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // execute方法，传入Runnable参数，无返回值
        pool.execute(() -> {
            log.debug("running");
        });

        // submit方法，传入Callable参数，使用Future接收返回值
        Future<String> future = pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                log.debug("running");
                return "ok";
            }
        });
        log.debug("{}", future.get());

        // invokeAll方法
        // 超时是 “给任务们整体执行设一个最大允许耗时”，不是 “任务入队等待执行的超时”，超时后未完成任务会被主动取消，取结果就会抛异常 。
        List<Future<Object>> futures = pool.invokeAll(Arrays.asList(
                () -> {
                    log.debug("running");
                    Thread.sleep(2000);
                    return "1";
                },
                () -> {
                    log.debug("running");
                    Thread.sleep(2000);
                    return "2";
                },
                () -> {
                    log.debug("running");
                    return "2";
                }
        ), 1000, TimeUnit.MILLISECONDS);
        // futures.forEach(f -> {
        //     try {
        //         log.debug("{}", f.get());
        //     } catch (InterruptedException e) {
        //         throw new RuntimeException(e);
        //     } catch (ExecutionException e) {
        //         throw new RuntimeException(e);
        //     }
        // });

        System.out.println("-----------------------------------");
        // invokeAny，执行一个方法，并在执行完后立刻返回，其他任务丢弃
        Object res = pool.invokeAny(Arrays.asList(
                () -> {
                    log.debug("running");
                    Thread.sleep(1000);
                    return "1";
                },
                () -> {
                    log.debug("running");
                    Thread.sleep(1000);
                    return "2";
                },
                () -> {
                    log.debug("running");
                    Thread.sleep(1000);
                    return "3";
                }
        ));
        log.debug("{}", res);
        System.out.println("----------------------------------");
        pool.shutdown();
        pool.execute(() -> {
            log.debug("running");
        });
    }
}
