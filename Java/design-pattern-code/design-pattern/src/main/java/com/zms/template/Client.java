package com.zms.template;

public class Client {
    public static void main(String[] args) {
        // 炒包菜
        ConcreteClass_BaoCai concreteClassBaoCai = new ConcreteClass_BaoCai();
        // 调用整个炒菜算法
        concreteClassBaoCai.process();
    }
}
