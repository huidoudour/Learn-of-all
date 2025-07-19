package com.zms.proxy.static_proxy;

public class Client {
    public static void main(String[] args) {
        // 创建真实明星
        RealStar realStar = new RealStar();

        // 创建明星的代理（经纪人）
        AgentProxy proxy = new AgentProxy(realStar);

        // 客户端只通过代理访问明星
        proxy.signContract();
        proxy.perform();
        proxy.collectMoney();
    }
}
