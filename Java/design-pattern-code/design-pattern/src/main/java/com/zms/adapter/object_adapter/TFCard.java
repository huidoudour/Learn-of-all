package com.zms.adapter.object_adapter;

public interface TFCard {
    // 从TF卡读数据
    String readTF();

    // 向TF卡写数据
    void writeTF(String tf);
}
