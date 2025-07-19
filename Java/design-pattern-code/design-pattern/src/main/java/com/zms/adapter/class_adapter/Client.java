package com.zms.adapter.class_adapter;

public class Client {
    public static void main(String[] args) {
        Computer computer = new Computer();
        String string = computer.readSD(new SDCardImpl());
        System.out.println(string);

        // 使用计算机读取TFCard
        String string1 = computer.readSD(new SDAdapter());
        System.out.println(string1);

    }
}
