package com.zms.proxy.jdk_proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {
    // 目标对象
    private final RealStar realStar = new RealStar();

    // 获取代理对象
    public Star getProxyObject() {
        /**
         * ClassLoader loader：被代理类的类加载器
         * Class<?>[] interfaces：类代理类实现的所有接口
         * InvocationHandler h：调用处理程序，用于将方法调用分派
         */
        Star star = (Star) Proxy.newProxyInstance(
                realStar.getClass().getClassLoader(),
                realStar.getClass().getInterfaces(),
                new InvocationHandler() {
                    /**
                     * Object proxy：动态生成的代理类
                     * Method method：被代理类执行的方法
                     * Object[] args：被代理类执行方法的参数
                     */
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        // 方法调用前的增强逻辑
                        if (method.getName().equals("signContract")) {
                            System.out.println("经纪人：仔细审阅合同条款，确保明星利益！");
                        } else if (method.getName().equals("perform")) {
                            System.out.println("经纪人：安排行程，对接场地，确保演出顺利！");
                        }

                        // 调用真实对象的方法
                        // 第一个参数是真实对象，第二个参数是真实对象方法的参数
                        Object result = method.invoke(realStar, args);

                        // 方法调用后的增强逻辑
                        if (method.getName().equals("perform")) {
                            System.out.println("经纪人：演出结束，安排明星返程！");
                        } else if (method.getName().equals("collectMoney")) {
                            System.out.println("经纪人：扣除佣金，转账给明星！");
                        }

                        return result;
                    }
                }
        );
        return star;
    }
}
