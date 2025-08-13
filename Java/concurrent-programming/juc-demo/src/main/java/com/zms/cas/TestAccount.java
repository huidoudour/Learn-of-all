package com.zms.cas;

import java.util.concurrent.atomic.AtomicInteger;

public class TestAccount {
    public static void main(String[] args) {
        // UnSafeAccount unSafeAccount = new UnSafeAccount(10000);
        // Account.test(unSafeAccount, 10);


        // SafeAccountByLock safeAccountByLock = new SafeAccountByLock(10000);
        // Account.test(safeAccountByLock, 10);

        SafeAccountByCAS safeAccountByCAS = new SafeAccountByCAS(10000);
        Account.test(safeAccountByCAS, 10);
    }
}

// 线程不安全类
class UnSafeAccount implements Account{
    private int balance;

    public UnSafeAccount(int balance) {
        this.balance = balance;
    }

    // 查询余额
    public Integer getBalance() {
        return balance;
    }

    // 取款
    @Override
    public void withdraw(Integer money) {
        balance -= money;
    }
}


// 通过加锁的方式保证线程安全
class SafeAccountByLock implements Account{
    private int balance;

    public SafeAccountByLock(int balance) {
        this.balance = balance;
    }

    // 查询余额
    public synchronized Integer getBalance() {
        return balance;
    }

    // 取款
    @Override
    public synchronized void withdraw(Integer money) {
        balance -= money;
    }
}

// 通过CAS的方式保证线程安全
class SafeAccountByCAS implements Account{
    private AtomicInteger balance;

    public SafeAccountByCAS(int balance) {
        this.balance = new AtomicInteger(balance);
    }

    @Override
    public Integer getBalance() {
        return balance.get();
    }

    @Override
    public void withdraw(Integer money) {
        // while(true) {
        //     // 获取余额最新值
        //     int prev = balance.get();
        //     // 成功取款后的余额
        //     int next = prev - money;
        //     if(balance.compareAndSet(prev, next)) {
        //         break;
        //     }
        // }

        // 等价操作
        balance.getAndAdd(-1*money);
    }
}
