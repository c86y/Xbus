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

    static final boolean DEBUG = BuildConfig.DEBUG;

    private static final int DEFAULT_CONNECT_TIMEOUT = 1000;

    static final int DEFAULT_SO_TIMEOUT = 0;
    static final String PATH_UNKNOWN = "unknown";

    public static final byte STATE_HANDSHAKE_INIT = 0;
    public static final byte STATE_HANDSHAKE_WAIT = 1;
    public static final byte STATE_HANDSHAKE_OK = 2;

    private static final String METHOD_REQUEST_NAME = "requestName";
    private static final String METHOD_ACCEPT = "accept";

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

        private XBusBinder(int timeoutMillis) {
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

                handshake(mIn, mOut);

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

        private void handshake(MessageReader in, MessageWriter out) throws IOException {
            Message msg;
            byte state = STATE_HANDSHAKE_INIT;

            while (state != STATE_HANDSHAKE_OK) {
                switch (state) {
                    case STATE_HANDSHAKE_INIT:
                        msg = in.read();
                        if (msg == null) {
                            out.write(new MethodReturn(mPath, XBusUtils.getHostPath(mContext), -1, ErrorCode.E_READ_MSG));
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (!XBusUtils.getHostPath(mContext).equals(msg.getSource())) {
                            out.write(new MethodReturn(mPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_SOURCE));
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (msg.getType() != Message.MessageType.METHOD_CALL) {
                            out.write(new MethodReturn(mPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_TYPE));
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (!METHOD_REQUEST_NAME.equals(msg.getAction())) {
                            out.write(new MethodReturn(mPath, XBusUtils.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_ACTION));
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        out.write(new MethodReturn(mPath, XBusUtils.getHostPath(mContext), msg.getSerial()).setReturnValue(getPath()));
                        state = STATE_HANDSHAKE_WAIT;
                        break;
                    case STATE_HANDSHAKE_WAIT:
                        msg = in.read();
                        if (msg == null) {
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (!XBusUtils.getHostPath(mContext).equals(msg.getSource())) {
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (msg.getType() != Message.MessageType.METHOD_CALL) {
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        if (!METHOD_ACCEPT.equals(msg.getAction())) {
                            throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                        }

                        state = STATE_HANDSHAKE_OK;
                        break;
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
