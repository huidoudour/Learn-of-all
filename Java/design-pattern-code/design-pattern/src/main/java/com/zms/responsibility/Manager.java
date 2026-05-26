package com.zms.responsibility;

// 部门经理类
public class Manager extends Handler{
    public Manager() {
        super(Handler.NUM_ONE, Handler.NUM_THREE);
    }

    @Override
    protected void handle(LeaveRequest leaveRequest) {
        System.out.println(leaveRequest.getName() + "请假" + leaveRequest.getNum() + "， 理由：" + leaveRequest.getReason());
        System.out.println("部门经理审核：同意");
    }
}
