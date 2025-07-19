package com.zms.proxy.cglib_proxy;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

public class ProxyFactory implements MethodInterceptor {


    public RealStar getInstance() {
        // 创建Enhancer对象，相当于JDK代理中的Proxy
        Enhancer enhancer = new Enhancer();
        // 设置父类字节码
        enhancer.setSuperclass(RealStar.class);
        // 设置回调函数
        enhancer.setCallback(this);

        // 创建代理对象
        return (RealStar) enhancer.create();
    }

    /**
     * @param o           代理对象
     * @param method      目标对象的执行方法
     * @param objects     目标对象的执行方法的参数
     * @param methodProxy 方法代理对象，用于调用父类方法
     */
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        // 方法调用前的增强逻辑
        if (method.getName().equals("signContract")) {
            System.out.println("经纪人：仔细审阅合同条款，确保明星利益！");
        } else if (method.getName().equals("perform")) {
            System.out.println("经纪人：安排行程，对接场地，确保演出顺利！");
        }

        // 调用真实对象的方法
        // 第一个参数是代理对象，第二个参数是真实对象方法的参数，返回值就是目标方法返回值
        Object result = methodProxy.invokeSuper(o, objects);

        // 方法调用后的增强逻辑
        if (method.getName().equals("perform")) {
            System.out.println("经纪人：演出结束，安排明星返程！");
        } else if (method.getName().equals("collectMoney")) {
            System.out.println("经纪人：扣除佣金，转账给明星！");
        }

        return result;
    }
}
