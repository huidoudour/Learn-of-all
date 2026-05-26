package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class TestReentrantReadWriteLock {
    public static void main(String[] args) {
        Container my = new Container();
        new Thread(() -> {
            my.readObject();
        }, "t1").start();

        new Thread(() -> {
            my.writeObject("123");
        }, "t2").start();
    }
}

@Slf4j
class Container {
    private Object data;
    private ReentrantReadWriteLock rw = new ReentrantReadWriteLock();

    // 读锁
    private Lock readLock = rw.readLock();
    // 写锁
    private Lock writeLock = rw.writeLock();

    // 读操作
    public Object readObject() {
        log.debug("尝试获取读锁");
        readLock.lock();
        try {
            readLock.lock();
            log.debug("再次尝试获取读锁");
            log.debug("读取中...");
            return data;
        } finally {
            log.debug("释放读锁");
            readLock.unlock();
        }
    }

    // 写操作
    public void writeObject(Object data) {
        log.debug("尝试获取写锁");
        writeLock.lock();
        try {
            log.debug("写入中...");
            this.data = data;
        } finally {
            System.out.println("释放写锁");
            writeLock.unlock();
        }
    }
}
