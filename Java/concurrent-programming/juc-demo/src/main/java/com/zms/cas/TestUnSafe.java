package com.zms.cas;

import lombok.Data;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnSafe {
    public static void main(String[] args) throws Exception {
        Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Unsafe unsafe = (Unsafe) theUnsafe.get(null);
        System.out.println(unsafe);

        // 获取字段的偏移值（就是字段相对于对象起始地址的偏移值）
        long nameOffset = unsafe.objectFieldOffset(Student.class.getDeclaredField("name"));
        long ageOffset = unsafe.objectFieldOffset(Student.class.getDeclaredField("age"));

        Student stu = new Student();
        unsafe.compareAndSwapObject(stu, nameOffset, null, "laoda");
        unsafe.compareAndSwapLong(stu, ageOffset, 0, 10);

        System.out.println(stu);
    }
}

@Data
class Student {
    private String name;
    private int age;
}
