package com.bilibili.xbus.proxy;

import com.bilibili.xbus.Connection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RemoteInvocation
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class RemoteInvocation implements InvocationHandler {

    private final Connection mConn;

    public RemoteInvocation(Connection conn) {
        mConn = conn;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return mConn.remoteInvocation(proxy, method, args);
    }

    public static Object buildProxy(RemoteObject remoteObject, Connection conn) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(remoteObject.getClassName());
        return Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RemoteInvocation(conn));
    }
}
