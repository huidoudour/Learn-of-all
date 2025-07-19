package com.zms.builder.demo1;

// 共享单车构建者
public class SharingBuilder extends Builder {
    @Override
    public void buildFrame() {
        bike.setFrame("铝合金车架");
    }

    @Override
    public void buildSeat() {
        System.out.println("塑料车座");
    }

    @Override
    public Bike createBike() {
        return bike;
    }
}
