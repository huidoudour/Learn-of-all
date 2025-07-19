package com.zms.iterator;

public interface StudentIterator {

    // 判断是否还有元素
    boolean hasNext();

    // 返回下一个元素
    Student next();
}
