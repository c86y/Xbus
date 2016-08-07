/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Process;

import com.bilibili.xbus.message.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBus
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBus implements Runnable{

    private static final int DEFAULT_SO_TIMEOUT = 3000;

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
        public void marshalling(Message msg, OutputStream out) {
            try {
                ObjectOutputStream oos = new ObjectOutputStream(out);
                oos.writeObject(msg);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Message deMarshalling(InputStream in) {
            Message msg = null;
            try {
                ObjectInputStream ois = new ObjectInputStream(in);
                msg = (Message) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
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

    public void connect(CallbackHandler handler) throws XBusException {
        mCallbackHandler = handler;

        LocalSocket socket = new LocalSocket();
        LocalSocketAddress address = new LocalSocketAddress(mContext.getPackageName() + XBusDaemon.SOCKET_NAME);
        try {
            socket.connect(address);
            socket.setSoTimeout(DEFAULT_SO_TIMEOUT);
            if (!new MyXBusAuth().auth(socket)) {
                socket.close();
                throw new XBusException("Failed to auth");
            }

            min = new MessageReader(socket.getInputStream());
            mout = new MessageWriter(socket.getOutputStream());
            runing.set(true);
            new Thread(this).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                disconnect();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void send(Message msg) {
        if (mout != null) {
            mout.write(msg);
        }
    }

    private Message read() {
        Message msg = null;
        if (min != null) {
            msg = min.read();
        }
        return msg;
    }

    public void disconnect() throws IOException {
        runing.set(false);
        if (min != null) {
            min.close();
        }
        if (mout != null) {
            mout.close();
        }
    }

    @Override
    public void run() {
        while (runing.get()) {
            Message msg = read();
            if (msg != null && mCallbackHandler != null) {
                mCallbackHandler.handle(msg);
            }
        }
    }
}
