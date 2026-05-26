package com.zms.prototype.demo1;

public class Certificate implements Cloneable {
    // 获奖人姓名
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void show() {
        System.out.println(name + " 同学在该学期表现良好，特发此证");
    }


    @Override
    public Certificate clone() {
        try {
            Certificate clone = (Certificate) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
