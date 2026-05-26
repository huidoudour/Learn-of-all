package com.zms.factory.simple_factory;

public abstract class Coffee {

    public abstract String getName();

    // 加糖和加奶
    public void addSugar() {
        System.out.println("add sugar");
    }
    public void addMilk() {
        System.out.println("add milk");
    }
}
