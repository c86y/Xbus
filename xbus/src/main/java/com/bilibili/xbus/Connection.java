package com.bilibili.xbus;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.utils.XBusLog;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connection
 *
 * @author chengyuan
 */
public class Connection {

    private final AtomicBoolean mRunning = new AtomicBoolean(false);

    private final MessageReader mIn;
    private final MessageWriter mOut;
    private final CallHandler mCallHandler;
    private final String mPath;
    private final Reader mReader;
    private final Sender mSender;
    private final BlockingQueue<Message> mSendQueue =
            new LinkedBlockingQueue<>();

    public Connection(String path, CallHandler handler, MessageReader in, MessageWriter out) {
        mPath = path;
        mIn = in;
        mOut = out;
        mReader = new Reader(path);
        mSender = new Sender(path);

        mRunning.set(true);

        mReader.start();
        mSender.start();

        mCallHandler = handler;
        handler.onConnected(this);
    }

    public String getPath() {
        return mPath;
    }

    private class Sender extends Thread {

        Sender(String name) {
            super("Sender#" + name);
        }

        @Override
        public void run() {
            while (mRunning.get() && !isInterrupted()) {
                Message msg = null;
                try {
                    msg = mSendQueue.take();
                    msg.getStopWatch().split("call take");
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
            while (mRunning.get() && !isInterrupted()) {
                try {
                    Message msg = mIn.read();
                    if (msg == null) {
                        continue;
                    }

                    CallHandler handler = mCallHandler;
                    if (handler != null) {
                        handler.handleMessage(msg);
                    }
                } catch (IOException e) {
                    if (XBusLog.ENABLE) {
                        XBusLog.printStackTrace(e);
                    }

                    disconnect();
                }
            }
        }
    }

    public void send(Message msg) {
        msg.getStopWatch().split("call in-queue");
        mSendQueue.add(msg);
    }

    public void disconnect() {
        mRunning.set(false);
        mSendQueue.clear();
        mSender.interrupt();
        mReader.interrupt();

        CallHandler handler = mCallHandler;
        if (handler != null) {
            handler.onDisconnected();
        }
    }
}
