package com.zms.framework.context.support;

import com.zms.framework.beans.BeanDefinition;
import com.zms.framework.beans.MutablePropertyValues;
import com.zms.framework.beans.PropertyValue;
import com.zms.framework.beans.factory.support.BeanDefinitionRegistry;
import com.zms.framework.beans.factory.xml.XMLBeanDefinitionReader;
import com.zms.framework.util.StringUtils;

import java.lang.reflect.Method;

/**
 * @version v1.0
 * @ClassName: ClassPathXmlApplicationContext
 * @Description: IOC容器的具体实现类
 *               用于加载类路径下的xml格式的配置文件
 * @Author: zms
 */
public class ClassPathXmlApplicationContext extends AbstractApplicationContext{

    public ClassPathXmlApplicationContext(String configLocation) {
        this.configLocation = configLocation;
        // 构造解析器对象
        beanDefinitionReader = new XMLBeanDefinitionReader();
        try {
            refresh();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 根据bean对象名称获取bean
    @Override
    public Object getBean(String name) throws Exception {
        // 先从对象容器查看是否有bean对象
        Object obj = singletonObjects.get(name);
        if (obj != null) {
            return obj;
        }

        // 获取BeanDefinition对象
        BeanDefinitionRegistry registry = beanDefinitionReader.getBeanDefinitionRegistry();
        BeanDefinition beanDefinition = registry.getBeanDefinition(name);

        // 获取bean信息中的className
        String className = beanDefinition.getClassName();

        // 通过反射创建对象
        Class<?> clazz = Class.forName(className);
        obj = clazz.newInstance();

        // 给对象进行依赖注入
        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
        // 这两种写法都可以遍历 propertyValueList
        // for (PropertyValue propertyValue : propertyValues.getPropertyValues()) {
        //
        // }
        for (PropertyValue propertyValue : propertyValues) {
            // 获取name属性值
            String propertyName = propertyValue.getName();
            // 获取ref属性
            String propertyRef = propertyValue.getRef();
            // 获取value属性
            String value = propertyValue.getValue();
            if(propertyRef != null && !"".equals(propertyRef)) {
                // 填充引用类型依赖（不包含String）
                // 获取依赖的bean
                Object bean = getBean(propertyRef);
                // 拼接方法名
                String methodName = StringUtils.getSetterMethodFieldName(propertyName);
                // 获取所有的方法对象
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if(method.getName().equals(methodName)) {
                        // 执行该setter方法
                        method.invoke(obj, bean);
                    }
                }
            }

            if(value != null && !"".equals(value)) {
                // 拼接方法名
                String methodName = StringUtils.getSetterMethodFieldName(propertyName);

                // 获取method对象
                Method method = clazz.getMethod(methodName, String.class);
                method.invoke(obj, value);
            }
        }

        // 将对象存入容器
        singletonObjects.put(name, obj);

        // 返回对象
        return obj;
    }

    @Override
    public <T> T getBean(String name, Class<? extends T> clazz) throws Exception {
        Object bean = getBean(name);
        if(bean == null) {
            return null;
        }
        return clazz.cast(bean);
    }
}
