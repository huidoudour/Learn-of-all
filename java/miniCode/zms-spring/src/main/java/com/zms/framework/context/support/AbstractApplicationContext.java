package com.zms.framework.context.support;

import com.zms.framework.beans.factory.support.BeanDefinitionReader;
import com.zms.framework.beans.factory.support.BeanDefinitionRegistry;
import com.zms.framework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @version v1.0
 * @ClassName: AbstractApplicationContext
 * @Description: 容器接口的抽象子类
 *              singletonObjects：bean对象容器
 *              beanDefinitionReader：用于初始化BeanDefinition
 * @Author: zms
 */
public abstract class AbstractApplicationContext implements ApplicationContext {

    // 解析器变量
    protected BeanDefinitionReader beanDefinitionReader;

    // 存储bean对象的容器
    protected Map<String, Object> singletonObjects = new HashMap<String, Object>();

    // 配置文件路径变量
    protected String configLocation;

    public void refresh() throws Exception {
        // 加载BeanDefinition对象
        beanDefinitionReader.loadBeanDefinitions(configLocation);
        // 初始化bean
        finishBeanInitialization();
    }

    /**
     * 完成Bean的初始化，调用子类的 getBean方法，因为不同环境需要使用不同的方式去获取bean
     * @throws Exception 初始化Bean失败时抛出
     */
    private void finishBeanInitialization() throws Exception {
        // 获取注册表对象
        BeanDefinitionRegistry registry = beanDefinitionReader.getBeanDefinitionRegistry();

        // 获取BeanDefinition名称
        String[] beanDefinitionNames = registry.getBeanDefinitionNames();
        // 初始化所有Bean
        for (String beanDefinitionName : beanDefinitionNames) {
            getBean(beanDefinitionName);
        }
    }
}


























