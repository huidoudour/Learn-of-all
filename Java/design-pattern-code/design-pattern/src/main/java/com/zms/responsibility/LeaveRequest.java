package com.zms.responsibility;

// 请假条类
public class LeaveRequest {
    // 请假者姓名
    private String name;
    // 请假天数
    private int num;
    // 请假理由
    private String reason;

    public LeaveRequest(String name, int num, String reason) {
        this.name = name;
        this.num = num;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public int getNum() {
        return num;
    }

    public String getReason() {
        return reason;
    }
}
