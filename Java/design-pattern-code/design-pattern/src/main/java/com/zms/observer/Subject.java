package com.zms.observer;

// 抽象主题
public interface Subject {
    // 添加订阅者（添加观察者）
    void attach(Observer observer);

    // 删除订阅者
    void detach(Observer observer);

    // 通知订阅者更新消息
    void notifyObservers(String message);
}
