package com.zms.iterator;

public class Student {
    // 学生姓名
    private String name;
    // 学生学号
    private int number;

    public Student(String name, int number) {
        this.name = name;
        this.number = number;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", number=" + number +
                '}';
    }
}
