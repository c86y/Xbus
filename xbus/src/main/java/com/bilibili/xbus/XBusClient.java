/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Debug;

import com.bilibili.xbus.message.Error;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.utils.XBusLog;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBusClient
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBusClient {

    static final boolean DEBUG = false;

    public static final int DEFAULT_SO_TIMEOUT = 0;
    public static final String PATH_UNKNOWN = "unknown";

    public static final byte STATE_HANDSHAKE_INIT = 0;
    public static final byte STATE_HANDSHAKE_WAIT = 1;
    public static final byte STATE_HANDSHAKE_OK = 2;

    private static final String METHOD_REQUEST_NAME = "requestName";
    private static final String METHOD_ACCEPT = "accept";

    public interface CallbackHandler {

        void handle(Message msg);
    }

    private MessageReader mIn;
    private MessageWriter mOut;
    private AtomicBoolean mRunning = new AtomicBoolean(false);
    private Context mContext;
    private String mPath;

    private CallbackHandler mCallbackHandler;
    private Reader mReader;
    private Sender mSender;
    private final BlockingQueue<Message> mSendQueue =
            new LinkedBlockingQueue<>();

    public XBusClient(Context context, String path) {
        mContext = context.getApplicationContext();
        mPath = path;
        mReader = new Reader(mPath);
        mSender = new Sender(mPath);
    }

    public String getPath() {
        return mPath;
    }

    public void connect(CallbackHandler handler) {
        if (XBusClient.DEBUG) {
            Debug.waitForDebugger();
        }

        mCallbackHandler = handler;

        new Thread() {
            @Override
            public void run() {
                LocalSocket socket = new LocalSocket();
                LocalSocketAddress address = new LocalSocketAddress(XBusHost.getAddress(mContext));
                try {
                    socket.connect(address);
                    socket.setSoTimeout(DEFAULT_SO_TIMEOUT);

                    XBusAuth.AuthResult result = new FastAuth().auth(XBusAuth.MODE_CLIENT, socket);
                    if (result == null || !result.success) {
                        socket.close();
                        throw new XBusException("Failed to auth");
                    }

                    mOut = new MessageWriter(getPath(), socket.getOutputStream());
                    mIn = new MessageReader(getPath(), socket.getInputStream());

                    handshake(mIn, mOut);

                    mRunning.set(true);

                    mReader.start();
                    mSender.start();
                } catch (IOException e) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }

                    disconnect();
                }
            }
        }.start();
    }

    public void send(Message msg) {
        mSendQueue.add(msg);
    }

    public void disconnect() {
        mRunning.set(false);

        mSender.interrupt();
        mReader.interrupt();

        mSendQueue.clear();

        closeQuietly(mIn);
        closeQuietly(mOut);
    }

    private void handshake(MessageReader in, MessageWriter out) throws IOException {
        Message msg;
        byte state = STATE_HANDSHAKE_INIT;

        while (state != STATE_HANDSHAKE_OK) {
            switch (state) {
                case STATE_HANDSHAKE_INIT:
                    msg = in.read();
                    if (msg == null) {
                        out.write(new Error(getPath(), XBusHost.getPath(mContext), Error.ErrorCode.E_READ_MSG, -1));
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (!XBusHost.getPath(mContext).equals(msg.getSource())) {
                        out.write(new Error(getPath(), XBusHost.getPath(mContext), Error.ErrorCode.E_INVALID_MSG_SOURCE, msg.getSerial()));
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_CALL) {
                        out.write(new Error(getPath(), XBusHost.getPath(mContext), Error.ErrorCode.E_INVALID_MSG_TYPE, msg.getSerial()));
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (!METHOD_REQUEST_NAME.equals(msg.getAction())) {
                        out.write(new Error(getPath(), XBusHost.getPath(mContext), Error.ErrorCode.E_INVALID_MSG_ACTION, msg.getSerial()));
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    out.write(new MethodReturn(getPath(), XBusHost.getPath(mContext), msg.getSerial(), getPath()));
                    state = STATE_HANDSHAKE_WAIT;
                    break;
                case STATE_HANDSHAKE_WAIT:
                    msg = in.read();
                    if (msg == null) {
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (!XBusHost.getPath(mContext).equals(msg.getSource())) {
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (msg.getType() != Message.MessageType.METHOD_CALL) {
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    if (!METHOD_ACCEPT.equals(msg.getAction())) {
                        throw new XBusException("handshake failed when state = " + state);
                    }

                    state = STATE_HANDSHAKE_OK;
                    break;
            }
        }
    }

    private class Sender extends Thread {

        Sender(String name) {
            super("Sender#" + name);
        }

        @Override
        public void run() {
            while (mRunning.get()) {
                Message msg = null;
                try {
                    msg = mSendQueue.take();
                } catch (InterruptedException e) {
                    if (!mRunning.get()) {
                        return;
                    }
                }

                try {
                    mOut.write(msg);
                } catch (IOException e) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }

                    disconnect();
                    return;
                }
            }
        }
    }

    private class Reader extends Thread {

        Reader(String name) {
            super("Reader#" + name);
        }

        @Override
        public void run() {
            while (mRunning.get()) {
                try {
                    Message msg = mIn.read();
                    if (msg == null) {
                        continue;
                    }

                    if (mCallbackHandler != null) {
                        mCallbackHandler.handle(msg);
                    }
                } catch (IOException e) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }

                    disconnect();
                    return;
                }
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
}
