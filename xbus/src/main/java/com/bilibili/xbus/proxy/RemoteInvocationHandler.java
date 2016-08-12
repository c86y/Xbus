package com.bilibili.xbus.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RemoteInvocation
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class RemoteInvocationHandler implements InvocationHandler {


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }

    public static Object buildProxy(RemoteObject remoteObject) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(remoteObject.getClassName());
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RemoteInvocationHandler());
    }
}
