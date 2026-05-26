package com.zms.adapter.object_adapter;

import com.zms.adapter.class_adapter.TFCardImpl;

public class Client {
    public static void main(String[] args) {
        // 读取SDCard
        Computer computer = new Computer();
        String string = computer.readSD(new SDCardImpl());
        System.out.println(string);

        // 使用计算机读取TFCard
        // 适配器类是SDCard的子类，其成员又有TFCard
        // 计算机读取sd卡数据调用SDCard.readSDCard()，所以调用Adapter.readSDCard()，最终读取TF卡
        SDAdapter sdAdapter = new SDAdapter(new TFCardImpl());
        String string1 = computer.readSD(sdAdapter);
        System.out.println(string1);

    }
}
