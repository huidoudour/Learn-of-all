package com.zms.cas;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TestLongAdder {

    public static void main(String[] args) {
        demo(
                ()->new AtomicLong(0),
                (addr)->addr.incrementAndGet()
        );

        demo(
                ()->new LongAdder(),
                (addr)->addr.increment()
        );
    }

    private static <T> void demo(Supplier<T> supplier, Consumer<T> consumer) {
        // 获取累加器
        T adder = supplier.get();
        // 线程集合
        List<Thread> threads = new ArrayList<>();
        // 4个线程，每个线程累加50万
        for(int i = 0; i < 4; i++) {
            threads.add(new Thread(() -> {
                for(int j = 0; j < 500000; j++) {
                    consumer.accept(adder);
                }
            }));
        }

        long start = System.nanoTime();
        // 启动所有线程
        for(Thread thread : threads) {
            thread.start();
        }
        // 等待所有线程结束
        for(Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long end = System.nanoTime();
        // 查看计数器最终结果和计数耗费时间
        System.out.println(adder + " cost: " + (end - start) / 1_000_000);

    }
}
