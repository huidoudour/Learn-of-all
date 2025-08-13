package com.zms.facade;

public class Client {
    public static void main(String[] args) {
        SmartApplicationFacade facade = new SmartApplicationFacade();
        facade.say("打开家电");
        System.out.println("---------------------");
        facade.say("关闭家电");
    }
}
