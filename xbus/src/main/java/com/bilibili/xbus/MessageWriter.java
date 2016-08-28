/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.utils.XBusLog;
import com.bilibili.xbus.utils.XBusUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * MessageWriter
 *
 * @author chengyuan
 */
public class MessageWriter implements Closeable {

    private String mAddress;
    private ObjectOutputStream mOut;

    public MessageWriter(String address, OutputStream out) {
        try {
            this.mAddress = address;
            this.mOut = new ObjectOutputStream(out);
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }

            XBusUtils.closeQuietly(this);
        }
    }

    public void write(Message msg) throws IOException {
        if (mOut == null) {
            throw new XBusException("Output stream is closed!");
        }

        if (XBusLog.ENABLE) {
            XBusLog.d(mAddress + " write msg: " + msg);
        }

        mOut.writeUnshared(msg);
        mOut.flush();
    }

    @Override
    public void close() throws IOException {
        mOut.close();
        mOut = null;
    }
}
