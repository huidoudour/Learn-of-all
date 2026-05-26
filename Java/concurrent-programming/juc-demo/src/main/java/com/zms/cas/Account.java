package com.zms.cas;

import java.util.ArrayList;
import java.util.List;

public interface Account {
    // 获取余额方法
    Integer getBalance();

    // 取款方法
    void withdraw(Integer money);

    /**
     * 启动1000个线程进行取款操作测试
     */
    static void test(Account account, Integer money) {
        List<Thread> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new Thread(() -> account.withdraw(money)));
        }

        list.forEach(Thread::start);
        list.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        System.out.println("最终余额: " + account.getBalance());
    }
}
