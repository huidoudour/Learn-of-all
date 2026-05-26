package com.zms.proxy.cglib_proxy;

public class RealStar {

    public void signContract() {
        System.out.println("明星：合同太复杂，我不看了...");
    }


    public void perform() {
        System.out.println("明星：唱跳rap music"); // 核心业务
    }


    public void collectMoney() {
        System.out.println("明星：钱太多，数不过来...");
    }
}
