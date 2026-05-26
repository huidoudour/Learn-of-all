package com.zms.adapter.object_adapter;

import com.zms.adapter.class_adapter.TFCard;

// 适配器类
public class SDAdapter  implements SDCard {
    private TFCard tfCard;

    public SDAdapter(TFCard tfCard) {
        this.tfCard = tfCard;
    }

    @Override
    public String readSDCard() {
        System.out.println("adapter read tf card");
        return tfCard.readTF();
    }

    @Override
    public void writeSDCard(String sdCard) {
        System.out.println("adapter write tf card");
        tfCard.writeTF(sdCard);
    }
}
