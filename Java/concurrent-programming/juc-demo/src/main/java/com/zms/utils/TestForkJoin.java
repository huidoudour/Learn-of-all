package com.zms.utils;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class TestForkJoin {
    public static void main(String[] args) {
        ForkJoinPool pool = new ForkJoinPool();
        System.out.println(pool.invoke(new Fibonaci(300)));
    }

}
class Fibonaci extends RecursiveTask<Integer> {


    private int n;
    public Fibonaci(int n) {
        this.n = n;
    }

    /**
     * The main computation performed by this task.
     *
     * @return the result of the computation
     */
    @Override
    protected Integer compute() {
        if (n <= 1) {
            return n; // 终止条件
        }

        Fibonaci f1 = new Fibonaci(n - 1);
        f1.fork(); // 异步执行f1

        Fibonaci f2 = new Fibonaci(n - 2);
        int result2 = f2.compute(); // 当前线程直接计算f2

        int result1 = f1.join(); // 获取f1的结果（此时f1可能已完成）

        return result1 + result2;
    }
}
