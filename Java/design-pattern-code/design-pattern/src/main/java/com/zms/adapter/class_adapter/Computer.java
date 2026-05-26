package com.zms.adapter.class_adapter;

public class Computer {

    // 计算机读取SD卡
    public String readSD(SDCard sdCard) {
        if(sdCard == null) {
            throw new NullPointerException("SDCard is null");
        }
        return sdCard.readSDCard();
    }
}
