package com.zms.strategy;

/**
 * @version v1.0
 * @ClassName: Strategy
 * @Description: 具体策略类B
 * @Author: zms
 */
public class StrategyB implements Strategy {
    public void activity() {
        System.out.println("满100减20");
    }
}
