package com.zms.factory.simple_factory;

public class Client {
    public static void main(String[] args) {
        CoffeeStore coffeeStore = new CoffeeStore();

        Coffee amCoffee = coffeeStore.orderCoffee("美式");
        System.out.println(amCoffee);


    }
}
