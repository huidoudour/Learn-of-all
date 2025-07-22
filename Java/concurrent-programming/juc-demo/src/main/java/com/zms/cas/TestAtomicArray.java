package com.zms.cas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestAtomicArray {

    public static void main(String[] args) {
        demo(() -> new int[10], (array)->array.length,(array, index)->array[index]++, array-> System.out.println(Arrays.toString(array)));


        demo(() -> new AtomicIntegerArray(10), (array)->array.length(),(array, index)->array.getAndIncrement(index), array-> System.out.println(array));
        // demo(() -> new AtomicIntegerArray(10), AtomicIntegerArray::length, AtomicIntegerArray::getAndIncrement, System.out::println);
    }


    /**
     * 将 “操作逻辑” 作为参数传递给方法
     * @param arraySupplier     提供者。没有参数，有一个返回值（泛型是返回值类型）。get()用于获取结果
     * @param lengthFun         函数。一个参数，一个返回值（第一个泛型是参数类型，第二个泛型是返回值类型）。apply()将此函数应用于给定的参数
     * @param putConsumer       两个参数的消费者。两个参数，没有返回值。（第一个泛型是第一个参数类型，第二个泛型是第二个参数类型）。apply()对给定的参数执行此操作
     * @param printConsumer     消费者。一个参数，无返回值（泛型是参数类型）。apply()对给定的参数执行此操作
     * @param <T>
     */
    private static <T> void demo(Supplier<T> arraySupplier, Function<T, Integer> lengthFun, BiConsumer<T, Integer> putConsumer, Consumer<T> printConsumer) {
        // 线程集合
        List<Thread> threads = new ArrayList<>();
        // 数组
        T array = arraySupplier.get();
        // 数组长度
        int length = lengthFun.apply(array);
        for (int i = 0; i < length; i++) {
            // 每个线程对数组做10000次操作
            threads.add(new Thread(() -> {
                for(int j = 0; j < 10000; j++) {
                    putConsumer.accept(array, j%length);    // 将10000次操作分摊在数组中的每个元素
                }
            }));
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程结束
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        // 打印数组
        printConsumer.accept(array);
    }
}
