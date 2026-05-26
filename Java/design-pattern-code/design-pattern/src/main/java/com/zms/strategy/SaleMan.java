package com.zms.strategy;

// 环境类，售货员
public class SaleMan {
    // 聚合策略
    private Strategy strategy;

    public SaleMan(Strategy strategy) {
        this.strategy = strategy;
    }

    public void sale() {
        // 售货员展示促销活动
        strategy.activity();
    }
}
