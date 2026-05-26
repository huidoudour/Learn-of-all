package com.zms.proxy.static_proxy;

public class AgentProxy implements Star {
    private RealStar realStar;

    public AgentProxy(RealStar realStar) {
        this.realStar = realStar;
    }

    @Override
    public void signContract() {
        System.out.println("经纪人：仔细审阅合同条款，确保明星利益！"); // 前置增强
        realStar.signContract(); // 调用明星的方法
    }

    @Override
    public void perform() {
        System.out.println("经纪人：安排行程，对接场地，确保演出顺利！");
        realStar.perform(); // 明星只负责表演
        System.out.println("经纪人：演出结束，安排明星返程！"); // 后置增强
    }

    @Override
    public void collectMoney() {
        realStar.collectMoney(); // 明星收钱
        System.out.println("经纪人：扣除佣金，转账给明星！"); // 后置增强
    }
}
