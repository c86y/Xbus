/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Debug;

import com.bilibili.xbus.message.ErrorCode;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.proxy.RemoteCallHandler;
import com.bilibili.xbus.utils.XBusLog;
import com.bilibili.xbus.utils.XBusUtils;

import java.io.IOException;

/**
 * XBus
 *
 * @author chengyuan
 */
public class XBus {

    static final boolean DEBUG = false;

    private static final int DEFAULT_CONNECT_TIMEOUT = 1000;

    static final int DEFAULT_SO_TIMEOUT = 0;

    private MessageReader mIn;
    private MessageWriter mOut;
    private Context mContext;
    private LocalSocket mSocket;
    private Connection mConn;

    private final XBusAuth mAuth = new FastAuth();
    private final String mPath;
    private final CallHandlerWrapper mCallHandlerWrapper;

    public XBus(Context context, String path, CallHandler callHandler) {
        mContext = context.getApplicationContext();
        mPath = path;
        mCallHandlerWrapper = new CallHandlerWrapper(callHandler);
    }

    public String getPath() {
        return mPath;
    }

    public XBus registerCallHandler(RemoteCallHandler callHandler) {
        mCallHandlerWrapper.register(callHandler);
        return this;
    }

    public XBus unregisterCallHandler(RemoteCallHandler callHandler) {
        mCallHandlerWrapper.unregister(callHandler);
        return this;
    }

    public XBus connect() {
        return connect(0);
    }

    public XBus connect(final int timeoutMillis) {
        if (XBus.DEBUG) {
            Debug.waitForDebugger();
        }

        mSocket = new LocalSocket();
        new XBusBinder(timeoutMillis).start();
        return this;
    }

    private class XBusBinder extends Thread {

        private int mTimeoutMillis;
        private XBusHandshake handshake;

        private XBusBinder(int timeoutMillis) {
            handshake = XBusHandshakeImpl.instance(mContext);
            mTimeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_CONNECT_TIMEOUT;
        }

        @Override
        public void run() {
            try {
                tryConnect(mSocket, mTimeoutMillis);

                mSocket.setSoTimeout(DEFAULT_SO_TIMEOUT);

                XBusAuth.AuthResult result = mAuth.auth(XBusAuth.MODE_CLIENT, mSocket);
                if (result == null || !result.success) {
                    throw new XBusException("Failed to auth");
                }

                mOut = new MessageWriter(mPath, mSocket.getOutputStream());
                mIn = new MessageReader(mPath, mSocket.getInputStream());

                handshake.handshakeWithHost(getPath(),mIn, mOut);

                mConn = new Connection(mPath, mCallHandlerWrapper, mIn, mOut);
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                close();
                mCallHandlerWrapper.onDisconnected();
            }
        }

        private void tryConnect(LocalSocket socket, int timeoutMillis) throws IOException {
            try {
                socket.connect(new LocalSocketAddress(XBusUtils.getHostAddress(mContext), LocalSocketAddress.Namespace.ABSTRACT));
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                if (timeoutMillis <= 0) {
                    throw e;
                }

                try {
                    synchronized (this) {
                        this.wait(timeoutMillis);
                    }

                    tryConnect(socket, 0);
                } catch (InterruptedException e1) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }
                }
            }
        }
    }

    private void close() {
        if (mConn != null) {
            mConn.disconnect();
            mConn = null;
        }
        XBusUtils.closeQuietly(mIn);
        XBusUtils.closeQuietly(mOut);
        XBusUtils.closeQuietly(mSocket);
    }
}
