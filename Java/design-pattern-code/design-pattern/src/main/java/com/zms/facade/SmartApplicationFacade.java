package com.zms.facade;

public class SmartApplicationFacade {
    private TV tv;
    private AirCondition airCondition;
    private Light light;

    public SmartApplicationFacade() {
        tv = new TV();
        airCondition = new AirCondition();
        light = new Light();
    }

    // 语音控制
    public void say(String message) {
        if(message.contains("打开")) {
            on();
        } else if (message.contains("关闭")) {
            off();
        } else {
            System.out.println("我还听不懂你说的话呢");
        }
    }

    private void on() {
        tv.on();
        airCondition.on();
        light.on();
    }
    private void off() {
        tv.off();
        airCondition.off();
        light.off();
    }
}
