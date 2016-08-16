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
import com.bilibili.xbus.utils.XBusLog;

import java.io.Closeable;
import java.io.IOException;

/**
 * XBus
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBus {

    static final boolean DEBUG = false;

    public static final int DEFAULT_SO_TIMEOUT = 0;
    public static final String PATH_UNKNOWN = "unknown";
    public static final String HOST_SOCKET_NAME = ".XBusHost";

    public static final byte STATE_HANDSHAKE_INIT = 0;
    public static final byte STATE_HANDSHAKE_WAIT = 1;
    public static final byte STATE_HANDSHAKE_OK = 2;

    private static final String METHOD_REQUEST_NAME = "requestName";
    private static final String METHOD_ACCEPT = "accept";

    public interface CallHandler {

        void onConnect(Connection conn);

        void handleMessage(Message msg);

        void onDisconnect();
    }

    private MessageReader mIn;
    private MessageWriter mOut;
    private Context mContext;
    private final String mPath;
    private LocalSocket mSocket;
    private Connection mConn;

    public XBus(Context context, String path) {
        mContext = context.getApplicationContext();
        mPath = path;
    }

    public String getPath() {
        return mPath;
    }

    public static String getHostPath(Context context) {
        return getHostAddress(context);
    }

    public static String getHostAddress(Context context) {
        return context.getPackageName() + HOST_SOCKET_NAME;
    }

    public void connect() {

    }

    public void connect(final CallHandler handler) {
        if (XBus.DEBUG) {
            Debug.waitForDebugger();
        }
        mSocket = new LocalSocket();

        new Thread() {
            @Override
            public void run() {
                try {
                    LocalSocketAddress address = new LocalSocketAddress(XBus.getHostAddress(mContext));
                    mSocket.connect(address);
                    mSocket.setSoTimeout(DEFAULT_SO_TIMEOUT);

                    XBusAuth.AuthResult result = new FastAuth().auth(XBusAuth.MODE_CLIENT, mSocket);
                    if (result == null || !result.success) {
                        close();
                        throw new XBusException("Failed to auth");
                    }

                    mOut = new MessageWriter(mPath, mSocket.getOutputStream());
                    mIn = new MessageReader(mPath, mSocket.getInputStream());

                    handshake(mIn, mOut);

                    mConn = new Connection(mPath, handler, mIn, mOut);
                } catch (IOException e) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }

                    close();
                    handler.onDisconnect();
                }
            }
        }.start();
    }

    static void closeQuietly(LocalSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
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
                        out.write(new MethodReturn(mPath, XBus.getHostPath(mContext), -1, ErrorCode.E_READ_MSG));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!XBus.getHostPath(mContext).equals(msg.getSource())) {
                        out.write(new MethodReturn(mPath, XBus.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_SOURCE));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_CALL) {
                        out.write(new MethodReturn(mPath,XBus.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_TYPE));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!METHOD_REQUEST_NAME.equals(msg.getAction())) {
                        out.write(new MethodReturn(mPath, XBus.getHostPath(mContext), msg.getSerial(), ErrorCode.E_INVALID_MSG_ACTION));
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    out.write(new MethodReturn(mPath, XBus.getHostPath(mContext), msg.getSerial()).setReturnValue(getPath()));
                    state = STATE_HANDSHAKE_WAIT;
                    break;
                case STATE_HANDSHAKE_WAIT:
                    msg = in.read();
                    if (msg == null) {
                        throw new XBusException("handshake failed when state = " + state + " msg = " + msg);
                    }

                    if (!XBus.getHostPath(mContext).equals(msg.getSource())) {
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

    private void close() {
        if (mConn != null) {
            mConn.disconnect();
            mConn = null;
        }

        closeQuietly(mIn);
        closeQuietly(mOut);
        closeQuietly(mSocket);
    }
}
