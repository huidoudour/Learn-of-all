package com.zms.decorator;

// 具体装饰
public class WithEgg extends Garnish {
    public WithEgg(FastFood fastfood) {
        super(fastfood, 1, "鸡蛋");
    }

    // WithEgg -->  Garnish  -->  FastFood
    // 快餐的信息存储在抽象类Garnish中的成员变量Fastfood，鸡蛋信息存储在基类FastFood中的成员price和desc
    // getPrice()返回的是鸡蛋的价格，getFastfood().cost()返回的是快餐的价格
    @Override
    public int cost() {
        return getPrice() + getFastfood().cost();
    }
}
