package com.zms.responsibility;

// 部门经理类
public class GeneralManager extends Handler{
    public GeneralManager() {
        super(Handler.NUM_THREE, Handler.NUM_SEVEN);
    }

    @Override
    protected void handle(LeaveRequest leaveRequest) {
        System.out.println(leaveRequest.getName() + "请假" + leaveRequest.getNum() + "， 理由：" + leaveRequest.getReason());
        System.out.println("总经理审核：同意");
    }
}
