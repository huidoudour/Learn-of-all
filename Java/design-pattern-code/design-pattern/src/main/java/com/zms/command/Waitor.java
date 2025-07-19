package com.zms.command;

import java.util.ArrayList;
import java.util.List;

// 服务员类
public class Waitor {

    // 持有多个命令对象
    private List<Command> commandList = new ArrayList<Command>();

    // 将cmd对象存储到集合
    public void addCommand(Command command) {
        commandList.add(command);
    }

    // 服务员发起命令
    public void orderUp() {
        System.out.println("厨师，来活了");
        // 遍历集合
        for (Command command : commandList) {
            if (command != null) {
                command.excute();
            }
        }
    }
}
