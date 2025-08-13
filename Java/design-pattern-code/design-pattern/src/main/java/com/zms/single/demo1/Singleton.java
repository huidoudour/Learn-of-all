package com.zms.single.demo1;

public class Singleton {
    // 将构造方法私有化，防止通过构造方法创建新的对象
    private Singleton(){}

    // 声明的同时赋值。实现饿汉式单例
    private static Singleton instance = new Singleton();

    public static Singleton getInstance() {
        return instance;
    }
}
