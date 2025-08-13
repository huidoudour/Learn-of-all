package com.zms.adapter.object_adapter;

public class TFCardImpl implements TFCard {
    @Override
    public String readTF() {
        String msg = "TFCard message: hello TFCard";    // 演示数据
        return msg;
    }

    @Override
    public void writeTF(String tf) {
        System.out.println(tf);         // 将数据输出到控制台，模拟输出到TFCard
    }
}
