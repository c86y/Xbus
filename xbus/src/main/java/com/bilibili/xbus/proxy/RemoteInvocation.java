package com.bilibili.xbus.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RemoteInvocation
 *
 * @author chengyuan
 */
public class RemoteInvocation implements InvocationHandler {

    private final BaseRemoteCall mRemoteCallHandler;

    private RemoteInvocation(BaseRemoteCall remoteCallHandler) {
        mRemoteCallHandler = remoteCallHandler;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return mRemoteCallHandler.remoteInvoke(proxy, method, args);
    }

    public static <T> T getProxy(Class<T> clazz, RemoteCallHandler remoteCallHandler) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RemoteInvocation(remoteCallHandler));
    }

    static <T> T buildProxy(RemoteObject remoteObject, RemoteCallHandler remoteCallHandler) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(remoteObject.getClassName());
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, new RemoteInvocation(remoteCallHandler));
    }
}
