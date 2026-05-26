package com.zms.observer;

import java.util.ArrayList;
import java.util.List;

// 具体主题类
public class SubscriptionSubject implements Subject{
    // 存储观察者的集合
    private List<Observer> observers = new ArrayList<Observer>();

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        // 遍历集合
        for (Observer observer : observers) {
            // 调用观察者中的update方法
            observer.update(message);
        }
    }
}
