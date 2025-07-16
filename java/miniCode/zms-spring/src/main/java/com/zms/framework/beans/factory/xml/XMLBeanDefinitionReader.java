package com.zms.framework.beans.factory.xml;

import com.zms.framework.beans.BeanDefinition;
import com.zms.framework.beans.MutablePropertyValues;
import com.zms.framework.beans.PropertyValue;
import com.zms.framework.beans.factory.support.BeanDefinitionReader;
import com.zms.framework.beans.factory.support.BeanDefinitionRegistry;
import com.zms.framework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.List;

/**
 * @version v1.0
 * @ClassName: XMLBeanDefinitionReader
 * @Description: 对XML文件进行解析的类
 *              registry：注册表对象，注册表对象的子类内部有BeanDefinition容器
 * @Author: zms
 */
public class XMLBeanDefinitionReader implements BeanDefinitionReader {
    // 注册表对象
    private BeanDefinitionRegistry registry;

    public XMLBeanDefinitionReader() {
        registry = new SimpleBeanDefinitionRegistry();
    }

    @Override
    public BeanDefinitionRegistry getBeanDefinitionRegistry() {
        return registry;
    }

    @Override
    public void loadBeanDefinitions(String location) throws DocumentException {
        // 使用dom4j进行xml文件解析
        SAXReader saxReader = new SAXReader();

        // 获取类路径下的输入流
        InputStream resource = XMLBeanDefinitionReader.class.getClassLoader().getResourceAsStream(location);

        Document document = saxReader.read(resource);

        // 根据Document对象获取根标签对象（beans）
        Element root = document.getRootElement();
        // 获取根标签下的所有bean标签对象
        List<Element> elements = root.elements("bean");
        for (Element element : elements) {
            // id属性
            String id = element.attributeValue("id");
            // class属性
            String className = element.attributeValue("class");

            // 将id属性和class属性封装到BeanDefinition对象
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setId(id);
            beanDefinition.setClassName(className);

            // 创建MutablePropertyValues对象
            MutablePropertyValues propertyValues = new MutablePropertyValues();

            // 获取bean标签下的所有property标签对象
            List<Element> propertyList = element.elements("property");
            for (Element property : propertyList) {
                String name = property.attributeValue("name");
                String ref = property.attributeValue("ref");
                String value = property.attributeValue("value");
                PropertyValue propertyValue = new PropertyValue(name, ref, value);
                propertyValues.addPropertyValue(propertyValue);
            }
            // 将MutablePropertyValues添加到BeanDefinition
            beanDefinition.setPropertyValues(propertyValues);

            // 将BeanDefinition注册到BeanDefinitionRegistry
            registry.registerBeanDefinition(id, beanDefinition);
        }
    }
}
