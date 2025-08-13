package com.zms.responsibility;

// 抽象处理类
public abstract class Handler {
    protected final static int NUM_ONE = 1;
    protected final static int NUM_THREE = 3;
    protected final static int NUM_SEVEN = 7;

    // 领导处理的请假请求天数
    private int numStart;
    private int numEnd;

    // 后继者（上级领导）
    private Handler nextHandler;

    public Handler(int numStart) {
        this.numStart = numStart;
    }

    public Handler(int numStart, int numEnd) {
        this.numStart = numStart;
        this.numEnd = numEnd;
    }

    // 设置上级领导对象
    public void setNextHandler(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    // 各级领导处理请假条的方法
    protected abstract void handle(LeaveRequest leaveRequest);

    // 提交请假条
    public final void submit(LeaveRequest leaveRequest) {
        // 判断当前处理者是否有权限处理（天数在 [numStart, numEnd] 范围内）
        if (leaveRequest.getNum() >= numStart && leaveRequest.getNum() <= numEnd) {
            this.handle(leaveRequest); // 有权限则处理
            System.out.println("流程结束");
        } else if (this.nextHandler != null) {
            // 无权限则传递给下一级处理者
            this.nextHandler.submit(leaveRequest);
        } else {
            // 没有更高级别处理者，审批不通过
            System.out.println("请假天数超出最大权限，审批不通过");
        }
    }
}
