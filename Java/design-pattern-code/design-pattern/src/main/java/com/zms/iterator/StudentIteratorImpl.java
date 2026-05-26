package com.zms.iterator;

import java.util.List;

public class StudentIteratorImpl implements StudentIterator{
    private List<Student> studentList;
    private int position = 0;


    public StudentIteratorImpl(List<Student> studentList) {
        this.studentList = studentList;
    }

    @Override
    public boolean hasNext() {
        return position < studentList.size();
    }

    @Override
    public Student next() {
        Student student = studentList.get(position);
        position++;
        return student;
    }
}
