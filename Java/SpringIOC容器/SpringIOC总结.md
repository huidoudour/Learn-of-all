###自定义Spring IOC总结

- zms-spring是模拟IOC容器
- spring-demo用于测试模拟容器

**概念：**

- Bean容器：Map<String, Object> singletonObjects。是 AbstractApplicationContext类的属性，所以 Bean容器的初始化由 AbstractApplicationContext 和 它的子类（ClassPathXmlApplicationContext）完成

- BeanDefinition容器：Map<String, BeanDefinition> beanDefinitionMap。是 SimpleBeanDefinitionRegistry类的属性，所以 BeanDefinition容器的初始化由注入了 SimpleBeanDefinitionRegistry类的 XMLBeanDefinitionReader类完成



**容器初始化步骤：**

1. 调用 Spring容器构造函数，进入 ClassPathXmlApplicationContext类

2. 执行构造方法，创建解析器对象，这个解析器对象内部有注册表对象，注册表对象内部维护BeanDefinition容器
3. 调用（子类的AbstractApplicationContext） refresh方法，进入AbstractApplicationContext类，再调用（子类的XMLBeanDefinitionReader）loadBeanDefinitions方法，**加载所有的BeanDefinition**，这里面会用到dom4j工具类的API
4. 调用 finishBeanInitialization方法，这个方法会先获取所有BeanDefinition的name属性，然后调用子类的（ClassPathXmlApplicationContext）的 getBean方法**初始化所有的Bean**，这里会涉及到依赖注入，主要分引用类型依赖和字符串类型依赖
5. refresh方法 完成后，就已经实现了自动装配和依赖注入
6. 从 Bean容器获取 UserService 对象（实际返回UserServiceImpl），直接使用即可

