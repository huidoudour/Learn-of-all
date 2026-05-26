package com.zms.adapter.object_adapter;

// 目标接口
public interface SDCard {

    // 从SD卡读数据
    String readSDCard();

    // 向SDCard卡写数据
    void writeSDCard(String sdCard);
}
