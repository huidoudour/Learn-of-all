package com.zms.responsibility;

public class Client {
    public static void main(String[] args) {
        // 创建一个请假条对象
        LeaveRequest request = new LeaveRequest("小明", 9, "身体不适");

        // 创建各级领导
        GeneralManager generalManager = new GeneralManager();
        Manager manager = new Manager();
        GroupLeader groupLeader = new GroupLeader();

        // 设置处理者链
        groupLeader.setNextHandler(manager);
        manager.setNextHandler(generalManager);

        // 小明提交请求
        groupLeader.submit(request);
    }
}
