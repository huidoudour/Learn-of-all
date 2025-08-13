package com.zms.base;

public class TestMonitorService {
}

class Monitor {
    // 执行监控的线程对象
    private Thread thread;
    // 结束运行标志，默认false不终止，true终止
    private volatile boolean terminated = false;
    // 启动标志，用于实现单例监控
    private volatile boolean running = false;

    public void startup() {
        synchronized (this) {
            if (running) {
                return;
            }
            running = true;
        }
        thread = new Thread(() -> {
            while (!terminated) {

            }
        })
    }

    public void shutdown() {
        terminated = true;
        thread.interrupt();
    }
}
