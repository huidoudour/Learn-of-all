package com.zms.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class TestAQS {
    public static void main(String[] args) {
        // 创建自定义锁实例
        MyLock lock = new MyLock();

        // 创建并启动线程t1
        new Thread(() -> {
            lock.lock(); // 获取锁（若锁被占用则阻塞）
            try {
                log.debug("locking..."); // 成功获取锁后打印日志
                try {
                    Thread.sleep(1000); // 模拟业务操作
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                log.debug("unlocking..."); // 准备释放锁
                lock.unlock(); // 释放锁（必须在finally块中执行）
            }
        }, "t1").start();

        // 创建并启动线程t2（逻辑与t1相同）
        new Thread(() -> {
            lock.lock(); // 获取锁（若t1未释放，则在此阻塞）
            try {
                log.debug("locking..."); // t1释放锁后，t2获取锁并打印日志
                try {
                    Thread.sleep(1000); // 模拟业务操作
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } finally {
                log.debug("unlocking..."); // 准备释放锁
                lock.unlock(); // 释放锁
            }
        }, "t2").start();
    }
}

/**
 * 自定义不可重入独占锁实现
 */
class MyLock implements Lock {

    // 基于AQS实现的同步器
    class MySync extends AbstractQueuedSynchronizer {

        // 尝试获取锁（CAS操作）
        @Override
        protected boolean tryAcquire(int arg) {
            // 如果当前状态为0（未锁定），则通过CAS设置为1（锁定）
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread()); // 记录当前持有锁的线程
                return true; // 获取锁成功
            }
            return false; // 获取锁失败
        }

        // 尝试释放锁
        @Override
        protected boolean tryRelease(int arg) {
            setState(0); // 直接将状态设为0（不可重入锁无需检查持有线程）
            setExclusiveOwnerThread(null); // 清空持有线程记录
            return true; // 释放锁成功
        }

        // 判断当前锁是否被占用
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1; // 状态为1表示锁被占用
        }

        // 创建条件变量（用于线程间协作）
        protected Condition newCondition() {
            return new ConditionObject(); // 使用AQS内置的条件变量实现
        }
    }

    private final MySync sync = new MySync(); // 同步器实例

    // 阻塞式获取锁（获取不到则进入等待队列）
    @Override
    public void lock() {
        sync.acquire(1);
    }

    // 可中断式获取锁（获取过程中可被其他线程中断）
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    // 尝试非阻塞获取锁（立即返回结果）
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    // 带超时的尝试获取锁
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    // 释放锁（唤醒等待队列中的线程）
    @Override
    public void unlock() {
        sync.release(1);
    }

    // 获取条件变量
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
