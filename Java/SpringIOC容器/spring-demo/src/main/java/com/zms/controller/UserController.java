package com.zms.controller;

import com.zms.framework.context.ApplicationContext;
import com.zms.framework.context.support.ClassPathXmlApplicationContext;
import com.zms.service.UserService;

/**
 * @version v1.0
 * @ClassName: UserController
 * @Description: 测试入口
 * @Author: zms
 */
public class UserController {
    public static void main(String[] args) throws Exception {
        // 1.创建Spring容器
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");

        // 2.获取UserService对象
        UserService userService = context.getBean("userService", UserService.class);

        // 3.使用业务层代码
        userService.add();
    }
}
