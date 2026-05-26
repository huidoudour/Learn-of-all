package com.zms.cas;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class TestMyAtomic {
    public static void main(String[] args) {
        MyAtomicInteger myAtomicInteger = new MyAtomicInteger(1000);

        // 线程集合
        List<Thread> threads = new ArrayList<Thread>();
        for(int i = 0; i < 1000; i++) {
            threads.add(new Thread(() -> {
                myAtomicInteger.decrement();
            }));
        }
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

        // 打印结果
        System.out.println(myAtomicInteger);
        //MyAtomicInteger{value=0}
    }
}


class MyAtomicInteger {
    private volatile int value;
    private static final long valueOffset;
    private static final Unsafe UNSAFE;
    // 初始化UNSAFE和字段偏移值
    static {
        Field theUnsafe = null;
        try {
            theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            valueOffset = UNSAFE.objectFieldOffset(MyAtomicInteger.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public MyAtomicInteger(int value) {
        this.value = value;
    }

    public void decrement() {
        while (true) {
            // 期待值
            int prev = value;
            // 目标值
            int next = prev - 1;
            if (UNSAFE.compareAndSwapInt(this, valueOffset, prev, next)) {
                break;
            }
        }
    }

    @Override
    public String toString() {
        return "MyAtomicInteger{" +
                "value=" + value +
                '}';
    }
}
