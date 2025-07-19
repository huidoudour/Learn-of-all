package com.zms.strategy;

// 客户端
public class Client {
    public static void main(String[] args) {
        // 春节，春节促销活动
        SaleMan saleMan = new SaleMan(new StrategyA());
        saleMan.sale();

        // 中秋节，中秋节促销活动
        SaleMan saleMan1 = new SaleMan(new StrategyB());
        saleMan1.sale();
    }
}
