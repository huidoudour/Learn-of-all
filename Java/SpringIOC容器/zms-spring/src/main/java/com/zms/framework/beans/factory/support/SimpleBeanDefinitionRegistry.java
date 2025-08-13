package com.zms.framework.beans.factory.support;

import com.zms.framework.beans.BeanDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * @version v1.0
 * @ClassName: SimpleBeanDefinitionRegistry
 * @Description: 注册表接口子类
 *              beanDefinitionMap：BeanDefinition容器
 * @Author: zms
 */
public class SimpleBeanDefinitionRegistry implements BeanDefinitionRegistry {
    // BeanDefinition容器
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<String, BeanDefinition>();


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName, beanDefinition);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        beanDefinitionMap.remove(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinitionMap.get(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return beanDefinitionMap.containsKey(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return beanDefinitionMap.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return beanDefinitionMap.keySet().toArray(new String[0]);
    }
}
