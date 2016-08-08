/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Process;
import android.support.annotation.WorkerThread;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBus
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBus implements Runnable {

    private static final int DEFAULT_SO_TIMEOUT = 30000;

    public interface CallbackHandler {

        void handle(Message msg);
    }

    private MessageReader min;
    private MessageWriter mout;
    private AtomicBoolean runing = new AtomicBoolean(false);
    private Context mContext;
    private String mName;

    private CallbackHandler mCallbackHandler;

    static final Marshalling mMarshalling = new Marshalling() {
        @Override
        public void marshalling(Message msg, OutputStream out) throws IOException {
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(msg);
        }

        @Override
        public Message deMarshalling(InputStream in) throws IOException {
            Message msg = null;
            try {
                ObjectInputStream ois = new ObjectInputStream(in);
                msg = (Message) ois.readObject();
            } catch (ClassNotFoundException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }
            }
            return msg;
        }
    };

    public XBus(Context context) {
        mContext = context.getApplicationContext();
        mName = mContext.getPackageName() + "#" + Process.myUid() + "#" + Process.myPid();
    }

    public String getName() {
        return mName;
    }

    @WorkerThread
    public void connect(CallbackHandler handler) throws XBusException {
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

            min = new MessageReader(socket.getInputStream());
            mout = new MessageWriter(socket.getOutputStream());
            runing.set(true);
            new Thread(this).start();
        } catch (IOException e) {
            if (XBusLog.DEBUG) {
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
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }

                closeQuietly(mout);
                mout = null;
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
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }

                closeQuietly(min);
                min = null;
            }
        }
        return msg;
    }

    public void disconnect() {
        runing.set(false);
        closeQuietly(min);
        closeQuietly(mout);
    }

    private void handleMessage(Message msg) {
        String member = msg.getMember();
        if (member == null) {
            return;
        }

        if ("getName".equals(member)) {
            msg = new MethodReturn(Message.MessageType.METHOD_RETURN, getName(), XBusDaemon.getAddress(mContext), getName());
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

            if (XBusDaemon.getAddress(mContext).equals(msg.getSource())) {
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
            } catch (IOException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }
            }
        }
    }
}
