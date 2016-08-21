package com.bilibili.xbus.proxy;

import com.bilibili.xbus.CallHandler;

import java.lang.reflect.Method;

/**
 * Created by Kaede on 16/8/21.
 */
public abstract class BaseRemoteCall implements CallHandler {
    abstract Object remoteInvoke(final Object proxy, final Method method, final Object[] args) throws Throwable;
}
