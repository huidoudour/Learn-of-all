package com.zms.iterator;

// 抽象聚合类
public interface StudentAggregate {

    // 添加学生功能
    void add(Student student);

    // 删除学生功能
    void remove(Student student);

    // 返回迭代器
    StudentIterator getStudentIterator();
}
