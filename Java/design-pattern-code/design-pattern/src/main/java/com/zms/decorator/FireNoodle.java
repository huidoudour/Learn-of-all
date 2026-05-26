package com.zms.decorator;

// 具体构件
public class FireNoodle extends FastFood{

    public FireNoodle() {
        super(12, "炒面");
    }

    @Override
    public int cost() {
        return getPrice();
    }
}
