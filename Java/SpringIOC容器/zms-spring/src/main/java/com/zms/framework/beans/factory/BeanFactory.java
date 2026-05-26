package com.zms.framework.beans.factory;

/**
 * @version v1.0
 * @ClassName: BeanFactory
 * @Description: IOC容器父接口
 * @Author: zms
 */
public interface BeanFactory {
    // 根据bean对象的名称获取bean
    Object getBean(String name) throws Exception;
    // 根据bean对象的名称和类型，获取并强制转换类型
    <T> T getBean(String name, Class<? extends T> clazz) throws Exception;
}
