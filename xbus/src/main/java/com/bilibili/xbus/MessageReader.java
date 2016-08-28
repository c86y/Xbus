/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.utils.XBusLog;
import com.bilibili.xbus.utils.XBusUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * MessageReceiver
 *
 * @author chengyuan
 */
public class MessageReader implements Closeable {

    private String mAddress;
    private ObjectInputStream mIn;

    public MessageReader(String address, InputStream in) {
        try {
            this.mAddress = address;
            this.mIn = new ObjectInputStream(in);
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            XBusUtils.closeQuietly(this);
        }
    }

    public Message read() throws IOException {
        if (mIn == null) {
            throw new XBusException("Input stream is closed!");
        }

        Message msg = null;
        try {
            msg = (Message) mIn.readUnshared();

            if (XBusLog.ENABLE) {
                XBusLog.d(mAddress + " read msg: " + msg);
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
        mIn.close();
        mIn = null;
    }

}
