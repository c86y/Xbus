/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.utils.XBusLog;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * MessageReceiver
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class MessageReader implements Closeable {

    private String name;
    private ObjectInputStream in;

    public MessageReader(String name, InputStream in) {
        try {
            this.name = name;
            this.in = new ObjectInputStream(in);
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            XBus.closeQuietly(this);
        }
    }

    public Message read() throws IOException {
        if (in == null) {
            throw new XBusException("Input stream is closed!");
        }

        Message msg = null;
        try {
            msg = (Message) in.readObject();

            if (XBusLog.ENABLE) {
                XBusLog.d(name + " read msg: " + msg);
            }
        } catch (ClassNotFoundException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }
        }
        return msg;
    }

    @Override
    public void close() throws IOException {
        in.close();
        in = null;
    }

}
