package com.zms.decorator;

public class Client {
    public static void main(String[] args) {
        // 一份炒饭
        FastFood fireRice = new FireRice();
        System.out.println(fireRice.getDesc() + " " + fireRice.getPrice() + "元");

        // 在炒饭上面加一个鸡蛋
        WithEgg food = new WithEgg(fireRice);
        System.out.println(food.getDesc() + " " + food.cost() + "元");

        // 再加一个鸡蛋
        WithEgg with2Egg = new WithEgg(food);
        System.out.println(with2Egg.getDesc() + " " + with2Egg.cost());
    }
}
