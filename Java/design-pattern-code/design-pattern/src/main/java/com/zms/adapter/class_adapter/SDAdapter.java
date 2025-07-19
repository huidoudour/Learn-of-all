package com.zms.adapter.class_adapter;

// 适配器类
public class SDAdapter extends TFCardImpl implements SDCard{
    @Override
    public String readSDCard() {
        System.out.println("adapter read tf card");
        return readTF();
    }

    @Override
    public void writeSDCard(String sdCard) {
        System.out.println("adapter write tf card");
        writeTF(sdCard);
    }
}
