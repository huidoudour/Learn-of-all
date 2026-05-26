package com.zms.proxy.cglib_proxy;

public class Client {
    public static void main(String[] args) {
        // 获取代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        // 获取代理对象（实际上就是RealStar的子类）
        RealStar realStar = proxyFactory.getInstance();
        // 执行代理方法
        realStar.perform();
    }
}
