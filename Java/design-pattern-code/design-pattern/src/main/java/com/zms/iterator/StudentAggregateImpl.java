package com.zms.iterator;

import java.util.ArrayList;
import java.util.List;

// 抽象聚合实现类
public class StudentAggregateImpl implements StudentAggregate {
    private List<Student> students = new ArrayList<Student>();

    public StudentAggregateImpl() {
    }

    @Override
    public void add(Student student) {
        students.add(student);
    }

    @Override
    public void remove(Student student) {
        students.remove(student);
    }

    @Override
    public StudentIterator getStudentIterator() {
        return new StudentIteratorImpl(students);
    }
}
