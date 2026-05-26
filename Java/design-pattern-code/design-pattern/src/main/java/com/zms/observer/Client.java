package com.zms.observer;

public class Client {
    public static void main(String[] args) {
        // 创建公众号类
        SubscriptionSubject subject = new SubscriptionSubject();

        // 订阅公众号
        subject.attach(new WeiXinUser("小明"));
        subject.attach(new WeiXinUser("小李"));
        subject.attach(new WeiXinUser("小三"));

        // 公众号更新
        subject.notifyObservers("出新文章了！！！");
    }
}
