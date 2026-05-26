package com.zms.prototype;

public class Client {
    public static void main(String[] args) {
        MyClass myClass = new MyClass();

        // 调用clone方法
        MyClass clone = myClass.clone();

        System.out.println(myClass == clone);
    }
}
