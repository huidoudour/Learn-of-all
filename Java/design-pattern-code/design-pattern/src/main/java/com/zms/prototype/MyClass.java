package com.zms.prototype;

public class MyClass implements Cloneable{
    public MyClass() {
        System.out.println("原型对象创建成功！");
    }

    @Override
    public MyClass clone() {
        try {
            return (MyClass) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
