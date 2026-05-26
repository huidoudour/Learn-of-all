package com.zms.decorator;

// 抽象构件
public abstract class FastFood {
    // 快餐价格
    private int price;
    // 快餐描述
    private String desc;

    public FastFood() {
    }

    public FastFood(int price, String desc) {
        this.price = price;
        this.desc = desc;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    // 快餐价格计算，由子类实现
    public abstract int cost();
}
