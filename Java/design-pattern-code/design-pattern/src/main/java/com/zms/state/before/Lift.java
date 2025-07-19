package com.zms.state.before;

public class Lift implements ILift{
    // 电梯状态
    private int state;


    @Override
    public void setState(int state) {
        this.state = state;
    }

    // 开启电梯门方法
    @Override
    public void open() {
        switch (state) {
            case OPENING_STATE:
            case RUNNING_STATE:
                // 什么都不做
                break;
            case CLOSING_STATE:
            case STOPPING_STATE:
                // 开启电梯门
                System.out.println("开启电梯门...");
                // 电梯门状态为开启
                setState(OPENING_STATE);
                break;
        }
    }

    @Override
    public void close() {
        switch (state) {
            case CLOSING_STATE:
            case STOPPING_STATE:
            case RUNNING_STATE:
                // 什么都不做
                break;
            case OPENING_STATE:
                System.out.println("关闭电梯门...");
                setState(CLOSING_STATE);
                break;
        }
    }

    @Override
    public void run() {
        switch (state) {
            case RUNNING_STATE:
            case STOPPING_STATE:
            case OPENING_STATE:
                // 什么都不做
                break;
            case CLOSING_STATE:
                System.out.println("电梯运行中...");
                setState(RUNNING_STATE);
                break;
        }
    }

    @Override
    public void stop() {
        switch (state) {
            case RUNNING_STATE:
        }
    }
}
