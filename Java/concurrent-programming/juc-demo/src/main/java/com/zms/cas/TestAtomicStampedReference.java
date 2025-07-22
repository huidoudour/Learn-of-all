package com.zms.cas;

import java.util.concurrent.atomic.AtomicStampedReference;

public class TestAtomicStampedReference {
    static AtomicStampedReference<String> ref = new AtomicStampedReference<>("A", 0);

    public static void main(String[] args) {
        // 期待值
        String prev = ref.getReference();
        // 版本号
        int stamp = ref.getStamp();
        other();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // 尝试修改，如果ref已经被其他线程修改，直接放弃本次修改
        System.out.println("change A->C " + ref.compareAndSet(prev, "C", stamp, stamp + 1));

    }

    private static void other() {
        // 将原始值A改为B
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change A->B " + ref.compareAndSet(ref.getReference(), "B", stamp, stamp+1));
        }, "t1").start();

        // 又将中间值B改回原始值A
        new Thread(() -> {
            int stamp = ref.getStamp();
            System.out.println("change B->A " + ref.compareAndSet(ref.getReference(), "A", stamp, stamp+1));
        }, "t2").start();
    }
}
