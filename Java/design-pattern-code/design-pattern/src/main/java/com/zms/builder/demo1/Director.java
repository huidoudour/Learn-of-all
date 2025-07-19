package com.zms.builder.demo1;

// 指挥者类
public class Director {
    // Builder类型的变量
    private Builder builder;

    public Director(Builder builder) {
        this.builder = builder;
    }

    public Bike construct() {
        builder.buildFrame();
        builder.buildSeat();
        return builder.createBike();
    }
}
