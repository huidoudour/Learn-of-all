package com.zms.framework.beans.factory.support;

import com.zms.framework.beans.BeanDefinition;

/**
 * @version v1.0
 * @ClassName: BeanDefinitionRegistry
 * @Description: 注册表接口
 * @Author: zms
 */
public interface BeanDefinitionRegistry {

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

    public void removeBeanDefinition(String beanName);

    public BeanDefinition getBeanDefinition(String beanName);

    public boolean containsBeanDefinition(String beanName);

    public int getBeanDefinitionCount();

    public String[] getBeanDefinitionNames();
}
