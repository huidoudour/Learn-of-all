package com.zms.command;

import java.util.Map;

// 具体命令类
public class OrderCommand implements Command {

    // 持有接受者对象
    private Chef chef;
    private Order order;

    public OrderCommand(Chef chef, Order order) {
        this.chef = chef;
        this.order = order;
    }

    @Override
    public void excute() {
        System.out.println(order.getDiningTable() + "桌的订单开始制作");
        Map<String, Integer> foodDir = order.getFoodDir();
        for (String foodName : foodDir.keySet()) {
            chef.makeFood(foodName, foodDir.get(foodName));
        }
        System.out.println(order.getDiningTable() + "桌的订单制作完毕！！！");
    }
}
