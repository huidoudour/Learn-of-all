package com.zms.decorator;

// 具体构件
public class FireRice extends FastFood{

    public FireRice() {
        super(10, "炒饭");
    }

    @Override
    public int cost() {
        return getPrice();
    }
}
