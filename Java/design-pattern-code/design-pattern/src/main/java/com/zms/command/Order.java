package com.zms.command;

import java.util.HashMap;
import java.util.Map;

// 订单类
public class Order {
    // 餐桌号码
    private int diningTable;

    // 菜品和份数
    private Map<String, Integer> foodDir = new HashMap<String, Integer>();

    public int getDiningTable() {
        return diningTable;
    }

    public void setDiningTable(int diningTable) {
        this.diningTable = diningTable;
    }

    public Map<String, Integer> getFoodDir() {
        return foodDir;
    }

    public void setFoodDir(String food, int num) {
        foodDir.put(food, num);
    }
}
