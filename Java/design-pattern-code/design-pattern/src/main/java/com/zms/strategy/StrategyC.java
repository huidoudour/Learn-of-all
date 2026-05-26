package com.zms.strategy;

/**
 * @version v1.0
 * @ClassName: Strategy
 * @Description: 具体策略类C
 * @Author: zms
 */
public class StrategyC implements Strategy {
    public void activity() {
        System.out.println("清仓甩卖，全场8折");
    }
}
