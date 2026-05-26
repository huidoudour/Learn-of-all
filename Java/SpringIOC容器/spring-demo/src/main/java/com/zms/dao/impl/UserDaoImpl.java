package com.zms.dao.impl;

import com.zms.dao.UserDao;

/**
 * @version v1.0
 * @ClassName: UserDaoImpl
 * @Description: 用户数据接口实现类
 * @Author: zms
 */
public class UserDaoImpl implements UserDao {


    public UserDaoImpl() {
        System.out.println("UserDao 被实例化");
    }

    @Override
    public void add() {
        System.out.println("UserDao...");
    }
}
