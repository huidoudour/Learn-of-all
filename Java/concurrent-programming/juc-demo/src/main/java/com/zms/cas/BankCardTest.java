package com.zms.cas;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BankCardTest {
    private static volatile AtomicReference<BankCard> bankCard = new AtomicReference<>(new BankCard("laoda",0));

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                while(true) {
                    // 期待值
                    BankCard prev = bankCard.get();
                    // 目标值
                    BankCard newCard = new BankCard(prev.getAccountName(), prev.getMoney() + 100);
                    if(bankCard.compareAndSet(prev, newCard)) {
                        System.out.println(newCard);
                        break;
                    }
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}

class BankCard {

    private  String accountName;
    private  int money;

    public BankCard() {
    }

    // 构造函数初始化 accountName 和 money
    public BankCard(String accountName,int money){
        this.accountName = accountName;
        this.money = money;
    }
    // 不提供任何修改个人账户的 set 方法，只提供 get 方法
    public String getAccountName() {
        return accountName;
    }
    public int getMoney() {
        return money;
    }
    // 重写 toString() 方法， 方便打印 BankCard
    @Override
    public String toString() {
        return "BankCard{" +
                "accountName='" + accountName + '\'' +
                ", money='" + money + '\'' +
                '}';
    }
}
