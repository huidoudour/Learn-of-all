package com.zms.service.impl;

import com.zms.dao.UserDao;
import com.zms.service.UserService;

/**
 * @version v1.0
 * @ClassName: UserServiceImpl
 * @Description: 用户业务接口实现类
 * @Author: zms
 */
public class UserServiceImpl implements UserService {

    private String username;
    private String password;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public UserServiceImpl() {
        System.out.println("UserService 被实例化");
    }

    private UserDao userDao;
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void add() {
        System.out.println("UserService..."  + "username: " + username + ", password: " + password);
        userDao.add();
    }
}
