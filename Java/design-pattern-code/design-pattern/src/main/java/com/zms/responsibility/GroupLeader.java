package com.zms.responsibility;

// 小组长类
public class GroupLeader extends Handler{
    public GroupLeader() {
        super(0, Handler.NUM_ONE);
    }

    @Override
    protected void handle(LeaveRequest leaveRequest) {
        System.out.println(leaveRequest.getName() + "请假" + leaveRequest.getNum() + "， 理由：" + leaveRequest.getReason());
        System.out.println("小组长审批：同意");
    }
}
