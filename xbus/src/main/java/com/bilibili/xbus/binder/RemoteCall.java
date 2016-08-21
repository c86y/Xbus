package com.bilibili.xbus.binder;

import com.bilibili.xbus.Connection;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.proxy.BaseRemoteCall;

import java.lang.reflect.Method;

/**
 * Created by Kaede on 16/8/21.
 */
public class RemoteCall extends BaseRemoteCall {
    IXBus ixBus;

    @Override
    public void onConnected(Connection conn) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void handleMessage(Message msg) {

    }

    @Override
    public Object remoteInvoke(Object proxy, Method method, Object[] args) throws Throwable {
        return null;
    }
}
