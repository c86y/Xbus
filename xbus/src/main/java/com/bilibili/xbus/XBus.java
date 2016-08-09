/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Debug;
import android.os.Process;
import android.support.annotation.WorkerThread;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBus
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBus implements Runnable {

    static final boolean DEBUG = false;

    static final int DEFAULT_SO_TIMEOUT = 0;

    public interface CallbackHandler {

        void handle(Message msg);
    }

    private MessageReader min;
    private MessageWriter mout;
    private AtomicBoolean runing = new AtomicBoolean(false);
    private Context mContext;
    private String mName;

    private CallbackHandler mCallbackHandler;

    public XBus(Context context) {
        mContext = context.getApplicationContext();
        mName = mContext.getPackageName() + "#" + Process.myUid() + "#" + Process.myPid();
    }

    public String getName() {
        return mName;
    }

    @WorkerThread
    public void connect(CallbackHandler handler) throws XBusException {
        if (XBus.DEBUG) {
            Debug.waitForDebugger();
        }

        mCallbackHandler = handler;

        LocalSocket socket = new LocalSocket();
        LocalSocketAddress address = new LocalSocketAddress(XBusDaemon.getAddress(mContext));
        try {
            socket.connect(address);
            socket.setSoTimeout(DEFAULT_SO_TIMEOUT);
            if (!new FastXBusAuth().auth(socket)) {
                socket.close();
                throw new XBusException("Failed to auth");
            }

            mout = new MessageWriter(getName(), socket.getOutputStream());
            min = new MessageReader(getName(), socket.getInputStream());

            runing.set(true);
            new Thread(this).start();
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            disconnect();
        }
    }

    @WorkerThread
    public void send(Message msg) {
        if (mout != null) {
            try {
                mout.write(msg);
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                disconnect();
            }
        }
    }

    @WorkerThread
    private Message read() {
        Message msg = null;
        if (min != null) {
            try {
                msg = min.read();
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                disconnect();
            }
        }
        return msg;
    }

    public void disconnect() {
        runing.set(false);

        closeQuietly(min);
        min = null;

        closeQuietly(mout);
        mout = null;
    }

    private void handleMessage(Message msg) {
        String member = msg.getMember();
        if (member == null) {
            return;
        }

        if ("getName".equals(member)) {
            msg = new MethodReturn(getName(), XBusDaemon.getName(mContext), member, getName());
            send(msg);
        }
    }

    @Override
    public void run() {
        while (runing.get()) {
            Message msg = read();
            if (msg == null) {
                continue;
            }

            if (XBusDaemon.getName(mContext).equals(msg.getSource())) {
                handleMessage(msg);
                continue;
            }

            if (mCallbackHandler != null) {
                mCallbackHandler.handle(msg);
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
