package com.zms.base;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestMonitor {
    public static void main(String[] args) throws InterruptedException {
        // 创建监控包装器实例
        MonitorWrapper monitorWrapper = new MonitorWrapper();

        // 启动监控线程
        monitorWrapper.startup();

        // 主线程休眠2秒，模拟业务运行
        Thread.sleep(2000);

        // 关闭监控线程
        monitorWrapper.shutdown();
    }
}

@Slf4j
class MonitorWrapper {
    // 监控线程实例
    private Thread monitor;

    /**
     * 启动监控线程
     * 创建并启动一个守护线程，每秒执行一次监控任务
     * 线程会在接收到中断信号后优雅退出
     */
    public void startup() {
        monitor = new Thread(() -> {
            // 线程主循环
            while (true) {
                // 获取当前线程引用
                Thread current = Thread.currentThread();

                // 检查中断标志，若被中断则进行资源清理并退出
                if(current.isInterrupted()) {
                    log.debug("料理后事...");
                    break;
                }

                try {
                    // 线程休眠1秒，模拟监控周期
                    Thread.sleep(1000);

                    // 打印监控日志，表示监控正常运行
                    log.debug("监控运行...");
                } catch (InterruptedException e) {
                    // 捕获中断异常
                    e.printStackTrace();
                    // 设置中断标记，防止线程因处于睡眠状态被打断后打断标志被清除
                    current.interrupt();
                }
            }
        }, "monitor");

        // 启动线程
        monitor.start();
    }

    /**
     * 关闭监控线程
     * 通过中断机制通知监控线程停止运行
     */
    public void shutdown() {
        // 调用interrupt()方法设置线程的中断标志
        monitor.interrupt();
    }
}
