package com.zms.builder.demo1;

// 抽象构建者
public abstract class Builder {

    // Bike成员
    protected Bike bike = new Bike();

    // 构建车架
    public abstract void buildFrame();

    // 构建车座
    public abstract void buildSeat();

    // 组装车子
    public abstract Bike createBike();
}
