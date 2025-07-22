package com.zms.utils;

import lombok.extern.slf4j.Slf4j;


import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class TestThreadPool {

    public static void main(String[] args) {
        ThreadPool threadPool = new ThreadPool(TimeUnit.MILLISECONDS, 2, 1000, 5, (queue, task) -> {
            // 死等
            // queue.put(task);
            // 带超时等待
            // queue.offer(task, TimeUnit.SECONDS, 2);
            // 让调用者放弃执行
            // log.debug("放弃任务");
            // 调用者抛出异常
            // throw new RuntimeException("不干了");
            // 调用者自己执行任务
            task.run();
        });
        for (int i = 0; i < 5; i++) {
            int j = i;
            threadPool.execute(() -> {
                log.debug("{}", j);
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}


@Slf4j
class ThreadPool {
    // 任务队列
    private BlockingQueue<Runnable> taskQueue;

    // 线程集合
    private HashSet<Worker> works = new HashSet<Worker>();

    // 核心线程数
    private int coreSize;

    // 获取任务的超时时间
    private long timeout;

    private TimeUnit unit;

    // 拒绝策略
    private RejectPolicy<Runnable> rejectPolicy;

    /**
     * 初始化线程池
     * @param unit          超时时间单位
     * @param coreSize      核心线程数
     * @param timeout       获取任务的超时时间
     * @param capacity      阻塞队列的最大容量
     * @param rejectPolicy  拒绝策略
     */
    public ThreadPool(TimeUnit unit, int coreSize, long timeout, int capacity, RejectPolicy<Runnable> rejectPolicy) {
        this.unit = unit;
        this.coreSize = coreSize;
        this.timeout = timeout;
        taskQueue = new BlockingQueue<>(capacity);
        this.rejectPolicy = rejectPolicy;
    }

    /**
     * 执行任务方法
     * 若当前工作线程数少于核心线程数，创建新线程执行任务；否则尝试将任务放入队列
     */
    public void execute(Runnable task) {
        synchronized (works) {
            if(works.size() < coreSize) {
                Worker worker = new Worker(task);
                log.debug("新增worker：{}，执行任务：{}", worker, task);
                works.add(worker);
                worker.start();
            } else {
                taskQueue.tryPut(rejectPolicy, task);
            }
        }
    }

class Worker extends Thread {
        private Runnable task;
        public Worker(Runnable task) {
            this.task = task;
        }

        /**
         * Worker线程实现
         * 负责从任务队列中获取并执行任务
         */
        @Override
        public void run() {
            while(task != null || (task = taskQueue.poll(1, unit)) != null) {
                try {
                    log.debug("正在执行任务：{}", task);
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    task = null;
                }
            }
            synchronized (works) {
                log.debug("移除worker：{}", this);
                works.remove(this);
            }
        }
    }
}

@FunctionalInterface
interface RejectPolicy<T> {
    void reject(BlockingQueue<T> queue, T task);
}

/**
 * 阻塞队列
 * @param <T>
 */
@Slf4j
class BlockingQueue<T> {
    // 1.任务队列
    private Deque<T> queue = new ArrayDeque<>();

    // 2.锁
    private ReentrantLock lock = new ReentrantLock();

    // 3.生产者条件变量（如果消息队列已满，则不能存放新的任务，生产者不能创建新的任务，则阻塞）
    private Condition fullWaitSet = lock.newCondition();

    // 4.消费者条件变量（如果任务队列为空，则不能消费任务，消费者阻塞）
    private Condition emptyWaitSet = lock.newCondition();

    // 5.容量
    private int capacity;

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }


    /**
     * 从阻塞队列中获取任务，若队列为空则无限期阻塞等待，直到有任务加入队列
     *
     * <p>核心逻辑：采用"生产者-消费者"模型的阻塞机制，通过锁和条件变量实现线程间的同步协作
     * 当队列中无任务时，当前线程会释放锁并进入等待状态，直到其他线程向队列中添加任务后被唤醒
     *
     * <p>执行步骤：
     * 1. 获取独占锁，确保队列操作的线程安全性
     * 2. 循环判断队列是否为空（使用while而非if，防止虚假唤醒导致的逻辑错误）
     *    - 若为空：当前线程进入emptyWaitSet条件变量的等待集，释放锁并阻塞等待
     *    - 若不为空：从队列头部移除并返回第一个任务
     * 3. 任务出队后，唤醒可能因队列满而阻塞的生产者线程（通过fullWaitSet条件变量）
     * 4. 无论是否获取到任务，最终都会释放锁，保证其他线程可以操作队列
     *
     * <p>异常处理：
     * 若等待过程中线程被中断（InterruptedException），会将异常包装为RuntimeException抛出
     * 上层调用者可根据需要捕获并处理中断情况
     *
     * @return 从队列头部获取的任务（非null，因为被唤醒时队列一定非空）
     * @throws RuntimeException 当等待被中断时抛出，包含原始InterruptedException
     */
    public T take(){
        lock.lock();
        try {
            while (queue.isEmpty()){
                try {
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带超时机制的任务获取方法
     * 尝试从队列头部获取任务，若队列为空则阻塞等待指定时间，超时后仍无任务则返回null
     *
     * <p>核心逻辑：
     * 1. 将超时时间转换为纳秒精度进行计算
     * 2. 使用while循环防止虚假唤醒，确保条件检查的原子性
     * 3. 通过awaitNanos方法实现带超时的等待，返回剩余等待时间
     * 4. 超时后直接返回null，避免无限期阻塞
     *
     * <p>执行流程：
     * 1. 获取锁后检查队列是否为空
     * 2. 若为空且未超时，进入等待状态并记录剩余时间
     * 3. 被唤醒后重新检查队列状态和剩余时间
     * 4. 超时则返回null，否则获取任务并唤醒生产者
     *
     * @param timeout 最大等待时间
     * @param unit 时间单位
     * @return 若在超时前获取到任务则返回任务，否则返回null
     * @throws RuntimeException 当等待被中断时抛出
     */
    public T poll(long timeout, TimeUnit unit){
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while (queue.isEmpty()){
                try {
                    if(nanos <= 0){
                        return null;
                    }
                    nanos = emptyWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 无限等待的任务添加方法
     * 将任务添加到队列尾部，若队列已满则阻塞等待，直到队列有空间
     *
     * <p>核心逻辑：
     * 1. 使用while循环检查队列容量，防止虚假唤醒
     * 2. 通过fullWaitSet条件变量实现生产者等待
     * 3. 入队后唤醒可能阻塞的消费者线程
     *
     * <p>执行流程：
     * 1. 获取锁后检查队列是否已满
     * 2. 若已满则进入等待状态，释放锁
     * 3. 被唤醒后重新检查队列状态
     * 4. 将任务添加到队列尾部，唤醒可能等待的消费者
     *
     * @param t 待添加的任务
     * @throws RuntimeException 当等待被中断时抛出
     */
    public void put(T t){
        lock.lock();
        try {
            while(queue.size() == capacity){
                try {
                    log.debug("等待添加任务：{}", t);
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            queue.addLast(t);
            emptyWaitSet.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带超时机制的任务入队方法
     * 尝试将任务添加到阻塞队列中，若队列已满则等待指定时间，超时后仍无法入队则返回失败
     *
     * <p>核心逻辑：采用超时等待机制平衡生产者与消费者的速度，避免线程无限期阻塞
     * 当队列已满时，当前线程会释放锁并等待，直到被唤醒（有空间时）或超时
     *
     * <p>执行步骤：
     * 1. 获取独占锁，确保队列操作的线程安全性
     * 2. 循环判断队列是否已满（使用while防止虚假唤醒）
     *    - 若已满：计算剩余等待时间，进入fullWaitSet条件变量等待
     *      - 若等待超时（nanos <= 0）：记录日志并返回false，表示任务入队失败
     *      - 若等待被唤醒：重新检查队列是否已满，重复判断
     *    - 若未满：将任务添加到队列尾部，唤醒可能因队列空而阻塞的消费者线程
     * 3. 返回true表示任务成功入队
     *
     * <p>异常处理：
     * 若等待过程中线程被中断（InterruptedException），会将异常包装为RuntimeException抛出
     *
     * @param t 待添加的任务
     * @param unit 超时时间的单位
     * @param timeout 超时时间长度
     * @return 任务是否成功入队（true：入队成功；false：超时失败）
     * @throws RuntimeException 当等待被中断时抛出，包含原始InterruptedException
     */
    public boolean offer(T t, TimeUnit unit, long timeout){
        lock.lock();
        try {
            long nanos = unit.toNanos(timeout);
            while(queue.size() == capacity){
                try {
                    log.debug("等待添加任务：{}", t);
                    if(nanos <= 0){
                        log.debug("丢弃任务：{}", t);
                        return false;
                    }
                    nanos = fullWaitSet.awaitNanos(nanos);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            queue.addLast(t);
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 带拒绝策略的任务入队方法
     * 尝试将任务添加到阻塞队列，若队列已满则通过指定的拒绝策略处理任务
     *
     * <p>核心逻辑：提供灵活的任务处理机制，当队列无法容纳新任务时，将处理逻辑委托给外部传入的拒绝策略
     * 避免固定的丢弃或阻塞行为，支持自定义处理（如重试、记录日志、交给其他线程处理等）
     *
     * <p>执行步骤：
     * 1. 获取独占锁，确保队列操作的线程安全性
     * 2. 判断队列是否已满
     *    - 若已满：调用拒绝策略（RejectPolicy）的reject方法，由外部逻辑处理该任务
     *    - 若未满：将任务添加到队列尾部，唤醒可能因队列空而阻塞的消费者线程
     *
     * @param rejectPolicy 当队列已满时的任务拒绝策略
     * @param task 待添加的任务
     */
    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            if(queue.size() == capacity){
                rejectPolicy.reject(this, task);
            } else {
                log.debug("加入任务队列：{}", task);
                queue.addLast(task);
                emptyWaitSet.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    // 8.获取大小
    public int size(){
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

}
