package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.proxy.RemoteCallHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * CallHandlerWrapper
 *
 * @author chengyuan
 */
public final class CallHandlerWrapper implements CallHandler {

    private final CallHandler mGlobalCallHandler;
    private final Map<String, RemoteCallHandler> mCallHandlers;

    CallHandlerWrapper(CallHandler callHandler) {
        mGlobalCallHandler = callHandler;
        mCallHandlers = new HashMap<>();
    }

    public void register(RemoteCallHandler handler) {
        synchronized (mCallHandlers) {
            mCallHandlers.put(handler.getDest(), handler);
        }
    }

    public void unregister(RemoteCallHandler handler) {
        synchronized (mCallHandlers) {
            mCallHandlers.remove(handler);
        }
    }

    @Override
    public void onConnected(Connection conn) {
        mGlobalCallHandler.onConnected(conn);
        synchronized (mCallHandlers) {
            for (CallHandler handler : mCallHandlers.values()) {
                handler.onConnected(conn);
            }
        }
    }

    @Override
    public void onDisconnected() {
        mGlobalCallHandler.onDisconnected();
        synchronized (mCallHandlers) {
            for (CallHandler handler : mCallHandlers.values()) {
                handler.onDisconnected();
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg == null) {
            return;
        }

        mGlobalCallHandler.handleMessage(msg);

        CallHandler handler;
        synchronized (mCallHandlers) {
            handler = mCallHandlers.get(msg.getSource());
        }

        if (handler == null) {
            return;
        }

        handler.handleMessage(msg);
    }
}
