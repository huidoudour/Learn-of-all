package com.zms.factory.simple_factory;

// 简单咖啡工厂
public class SimpleFactory {
    public Coffee createCoffee(String type) {
        Coffee coffee = null;
        switch (type) {
            case "美式":
                coffee = new AmericanCoffee();
                break;
            case "拿铁":
                coffee = new LatterCoffee();
                break;
            default:
                throw new RuntimeException("没有您要的咖啡");
        }
        return coffee;
    }
}
