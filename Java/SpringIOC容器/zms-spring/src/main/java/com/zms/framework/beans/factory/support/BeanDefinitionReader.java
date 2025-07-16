package com.zms.framework.beans.factory.support;

import org.dom4j.DocumentException;

/**
 * @version v1.0
 * @ClassName: BeanDefinitionReader
 * @Description: 用于解析BeanDefinition的规范接口
 * @Author: zms
 */
public interface BeanDefinitionReader {
    // 获取注册表对象
    BeanDefinitionRegistry getBeanDefinitionRegistry();

    // 加载配置文件并在注册表中注册
    void loadBeanDefinitions(String location) throws DocumentException;
}
