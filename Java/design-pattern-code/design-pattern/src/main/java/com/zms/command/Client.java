package com.zms.command;

public class Client {
    public static void main(String[] args) {
        // 创建订单对象
        Order order = new Order();
        order.setDiningTable(2);
        order.setFoodDir("炒鱿鱼", 1);
        order.setFoodDir("宫保鸡丁", 2);

        // 第二个订单
        Order order1 = new Order();
        order1.setDiningTable(3);
        order1.setFoodDir("西红柿炒鸡蛋", 1);

        // 厨师
        Chef chef = new Chef();

        // 命令对象
        OrderCommand orderCommand = new OrderCommand(chef, order);
        OrderCommand orderCommand1 = new OrderCommand(chef, order1);

        // 服务员
        Waitor waitor = new Waitor();
        waitor.addCommand(orderCommand);
        waitor.addCommand(orderCommand1);

        // 调用
        waitor.orderUp();
    }
}
