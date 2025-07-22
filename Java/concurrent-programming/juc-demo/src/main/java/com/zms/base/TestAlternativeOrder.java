package com.zms.base;

import java.util.concurrent.locks.LockSupport;

// 使用park、unpark实现交替打印
public class TestAlternativeOrder {
    private static Thread a;
    private static Thread b;
    private static Thread c;

    public static void main(String[] args) {
        AlternativeOrder order = new AlternativeOrder(5);

        a = new Thread(() -> {
            order.print("a", b);
        }, "a");

        b = new Thread(() -> {
            order.print("b", c);
        }, "b");

        c = new Thread(() -> {
            order.print("c", a);
        }, "c");

        a.start();
        b.start();
        c.start();

        LockSupport.unpark(a);
    }
}
class AlternativeOrder {
    private int loopNumber;

    public AlternativeOrder(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void print(String str, Thread next) {
        for (int i = 0; i < loopNumber; i++) {
            LockSupport.park();
            System.out.println(str);
            LockSupport.unpark(next);
        }
    }
}
