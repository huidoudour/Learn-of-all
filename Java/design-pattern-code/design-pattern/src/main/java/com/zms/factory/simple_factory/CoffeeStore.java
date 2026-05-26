package com.zms.factory.simple_factory;

public class CoffeeStore {
    private final SimpleFactory factory = new SimpleFactory();

    public Coffee orderCoffee(String type) {
        // 调用工厂的生产咖啡方法
        Coffee coffee = factory.createCoffee(type);

        // 加料
        coffee.addSugar();
        coffee.addMilk();
        return coffee;
    }

}
