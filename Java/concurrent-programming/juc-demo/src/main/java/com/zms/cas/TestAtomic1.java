package com.zms.cas;

import java.util.concurrent.atomic.AtomicInteger;

public class TestAtomic1 {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(0);
        System.out.println(atomicInteger.incrementAndGet());        // ++i
        System.out.println(atomicInteger.getAndIncrement());        // i++
        System.out.println(atomicInteger.get());
        System.out.println(atomicInteger.decrementAndGet());        // --i

        // updateAndGet函数参数是一个函数式接口，接口内方法的参数就是期待值，后面的就是目标值
        System.out.println(atomicInteger.updateAndGet(value -> value *10));
    }
}
