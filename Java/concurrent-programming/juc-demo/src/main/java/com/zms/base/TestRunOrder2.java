package com.zms.base;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.locks.LockSupport;

// 使用park、unpark
@Slf4j
public class TestRunOrder2 {


    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            LockSupport.park();
            log.debug("1");
        }, "t1");


        Thread t2 = new Thread(() -> {
            log.info("2");
            LockSupport.unpark(t1);
        }, "t2");

        t1.start();
        t2.start();
    }
}
