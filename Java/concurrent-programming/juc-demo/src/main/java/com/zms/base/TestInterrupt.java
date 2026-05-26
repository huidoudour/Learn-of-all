package com.zms.base;

public class TestInterrupt {
    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            while (true) {
                boolean interrupted = Thread.currentThread().isInterrupted();
                if(interrupted) {
                    return ;
                }
            }
        }, "t1");

        t1.start();
        System.out.println("t1的打断标记：" + t1.isInterrupted());
        t1.interrupt();
        System.out.println("t1的打断标记：" + t1.isInterrupted());
        System.out.println("t1的打断标记：" + t1.isInterrupted());
        System.out.println("t1的打断标记：" + t1.isInterrupted());
        System.out.println("t1的打断标记：" + t1.isInterrupted());
        System.out.println("t1的打断标记：" + t1.isInterrupted());

        

    }
}
