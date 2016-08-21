// IXBus.aidl
package com.bilibili.xbus.binder;

// Declare any non-default types here with import statements
import com.bilibili.xbus.binder.MethodReturn;
import com.bilibili.xbus.binder.MethodCall;

interface IXBus {
    MethodReturn remoteInvoke(in MethodCall methodCall);
}
