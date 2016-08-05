/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.bilibili.xbus.message.Message;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBus
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBus implements Runnable{

    public interface CallbackHandler {

        void handle(Message msg);
    }

    private MessageReader min;
    private MessageWriter mout;
    private AtomicBoolean runing = new AtomicBoolean(false);

    private CallbackHandler mCallbackHandler;

    public void connect(Context context, CallbackHandler handler) throws XBusExeception {
        LocalSocket socket = new LocalSocket();
        LocalSocketAddress address = new LocalSocketAddress(context.getPackageName() + XBusDaemon.SOCKET_NAME);
        try {
            socket.connect(address);
            if (!new MyXBusAuth().auth(socket)) {
                socket.close();
                throw new XBusExeception("Failed to auth");
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
