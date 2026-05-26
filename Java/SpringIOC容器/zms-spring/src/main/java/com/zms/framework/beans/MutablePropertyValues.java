package com.zms.framework.beans;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @version v1.0
 * @ClassName: MutablePropertyValues
 * @Description: 用于存储和管理多个PropertyValue对象
 * @Author: zms
 */
public class MutablePropertyValues implements Iterable<PropertyValue>{

    // 存储PropertyValue对象的List集合
    private final List<PropertyValue> propertyValueList;

    public MutablePropertyValues() {
        propertyValueList = new ArrayList<PropertyValue>();
    }

    public MutablePropertyValues(List<PropertyValue> propertyValues) {
        if(propertyValues == null) {
            propertyValueList = new ArrayList<PropertyValue>();
        } else {
            propertyValueList = propertyValues;
        }
    }

    // 将PropertyValue对象列表以数组的形式返回
    public PropertyValue[] getPropertyValues() {
        return propertyValueList.toArray(new PropertyValue[0]);
    }

    // 根据name属性获取PropertyValue对象，没有返回null
    public PropertyValue getPropertyValue(String propertyName) {
        for(PropertyValue propertyValue : propertyValueList) {
            if(propertyValue.getName().equals(propertyName)) {
                return propertyValue;
            }
        }
        return null;
    }

    // 添加PropertyValue对象
    public MutablePropertyValues addPropertyValue(PropertyValue propertyValue) {
        // 判断 propertyValue和集合中的对象的name属性是否重复，如果重复，则覆盖
        for(int i = 0; i < propertyValueList.size(); i++) {
            PropertyValue currentPv = propertyValueList.get(i);
            if(currentPv.getName().equals(propertyValue.getName())) {
                propertyValueList.set(i, propertyValue);
                return this;    // 实现链式编程
            }
        }
        propertyValueList.add(propertyValue);
        return this;    // 实现链式编程
    }

    // 判断集合中是否存在指定名称的PropertyValue对象
    public boolean containsPropertyValue(String propertyName) {
        return getPropertyValue(propertyName) != null;
    }

    // 判断集合是否为空
    public boolean isEmpty() {
        return propertyValueList.isEmpty();
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<PropertyValue> iterator() {
        return propertyValueList.iterator();
    }
}
