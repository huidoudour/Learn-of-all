package com.zms.proxy.static_proxy;

public class RealStar implements Star {
    @Override
    public void signContract() {
        System.out.println("明星：合同太复杂，我不看了...");
    }

    @Override
    public void perform() {
        System.out.println("明星：唱跳rap music"); // 核心业务
    }

    @Override
    public void collectMoney() {
        System.out.println("明星：钱太多，数不过来...");
    }
}
