package com.zms.iterator;

// 演示聚合对象和迭代器对象
public class Client {
    public static void main(String[] args) {
        // 聚合对象，这里的集合对象就相当于集合对象
        StudentAggregate studentAggregate = new StudentAggregateImpl();
        studentAggregate.add(new Student("laozhan", 1));
        studentAggregate.add(new Student("laoda", 2));
        studentAggregate.add(new Student("laoxiang", 3));

        // 获取迭代器对象
        StudentIterator studentIterator = studentAggregate.getStudentIterator();
        while (studentIterator.hasNext()) {
            Student student = studentIterator.next();
            System.out.println(student);
        }


    }
}
