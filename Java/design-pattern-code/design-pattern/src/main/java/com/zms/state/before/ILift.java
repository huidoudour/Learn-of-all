package com.zms.state.before;

// 电梯接口
public interface ILift {

    // 电梯的4个状态
    // OPENING的下一个状态只有CLOSING
    int OPENING_STATE = 0;
    // CLOSING的下一个状态可以是OPENING或RUNNING
    int CLOSING_STATE = 1;
    // RUNNING的下一个状态只能是STOOPING
    int RUNNING_STATE = 2;
    // STOPPING的下一个状态只能是OPENING
    int STOPPING_STATE = 3;

    // 设置电梯状态的功能
    void setState(int state);

    // 电梯操作功能
    void open();

    void close();

    void run();

    void stop();
}
