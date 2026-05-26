package com.zms.single.demo2;

public class Singleton {
    private Singleton() {}

    // 只声明，不赋值。实现懒汉式单例
    private static Singleton singleton;

    public static Singleton getInstance() {
        if(singleton == null) {
            singleton = new Singleton();
        }
        return singleton;
    }
}
