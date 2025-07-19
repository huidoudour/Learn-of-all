package com.zms.decorator;

// 抽象装饰器类
public abstract class Garnish extends FastFood{

    // 声明快餐中的变量
    private FastFood fastfood;

    public FastFood getFastfood() {
        return fastfood;
    }

    public void setFastfood(FastFood fastfood) {
        this.fastfood = fastfood;
    }

    public Garnish(FastFood fastfood, int price, String desc) {
        super(price, desc);
        this.fastfood = fastfood;
    }
}
