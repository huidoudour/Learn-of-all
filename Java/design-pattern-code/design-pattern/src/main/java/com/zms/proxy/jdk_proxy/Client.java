package com.zms.proxy.jdk_proxy;

public class Client {
    public static void main(String[] args) {
        // 获取工厂类
        ProxyFactory agentProxy = new ProxyFactory();
        // 获取代理类
        Star star = agentProxy.getProxyObject();
        // 执行代理方法
        star.perform();
    }
}
