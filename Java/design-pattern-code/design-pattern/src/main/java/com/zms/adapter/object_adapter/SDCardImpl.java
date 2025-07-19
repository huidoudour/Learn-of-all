package com.zms.adapter.object_adapter;

public class SDCardImpl implements SDCard {
    @Override
    public String readSDCard() {
        String msg = "SDCard message: hello SDCard";
        return msg;
    }

    @Override
    public void writeSDCard(String sdCard) {
        System.out.println(sdCard);
    }
}
