/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.bilibili.xbus.message.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBusDaemon
 *
 * @author chengyuan
 * @data 16/8/3.
 */
public class XBusDaemon extends Thread {

    private static final String TAG = "XBusDaemon";
    public static final String SOCKET_NAME = ".XBusDaemon";

    private Context mContext;
    private XBusRouter mRouter = new XBusRouter();

    private XBusAuth mXBusAuth = new MyXBusAuth();

    XBusDaemon(Context context) {
        super(TAG);
        mContext = context.getApplicationContext();
        new Dispatcher().start();
    }

    public void stopRunning() {
        mIsRunning.set(false);
    }


    @Override
    public synchronized void start() {
        mIsRunning.set(true);
        super.start();
    }

    @Override
    public void run() {
        LocalServerSocket lss = null;
        try {
            lss = new LocalServerSocket(mContext.getPackageName() + SOCKET_NAME);
            if (XBusLog.DEBUG) {
                XBusLog.d("XBus daemon is running");
            }

            while (mIsRunning.get()) {
                LocalSocket ls = lss.accept();

                if (XBusLog.DEBUG) {
                    XBusLog.d("accept socket: " + ls);
                }

                if (mXBusAuth.auth(ls)) {
                    mRouter.add(ls);
                } else {
                    ls.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (lss != null) {
                    lss.close();

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Dispatcher extends Thread {

        private static final String TAG = "Dispatcher";

        Dispatcher() {
            super(TAG);
        }

        @Override
        public void run() {
            Message msg = null;
            List<WeakReference<XBusPipe>> wps;
            synchronized (mRouter.inqueue) {
                try {
                    while (mRouter.inqueue.size() == 0) {
                        mRouter.inqueue.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                msg = mRouter.inqueue.head();
                wps = mRouter.inqueue.remove(msg);
            }

            if (wps != null) {
                for (WeakReference<XBusPipe> wp : wps) {
                    XBusPipe pipe = wp.get();

                    if (pipe == null) {
                        continue;
                    }

                    pipe.write(msg);
                }
            }
        }
    }

    class XBusRouter {

        private final Map<String, XBusPipe> pipes = new HashMap<>();
        final MagicMap<Message, WeakReference<XBusPipe>> inqueue = new MagicMap<>();
        final MagicMap<Message, WeakReference<XBusPipe>> outqueue = new MagicMap<>();

        void add(LocalSocket socket) {
            XBusPipe pipe = new XBusPipe(socket);
            synchronized (pipes) {
                pipes.put(pipe.name, pipe);
            }
        }

        void remove(XBusPipe pipe) {
            synchronized (pipes) {
                pipes.remove(pipe.name);
            }
        }
    }

    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    class XBusPipe implements Runnable {

        AtomicBoolean running = new AtomicBoolean(true);
        String name;
        LocalSocket socket;
        private MessageReader min;
        private MessageWriter mout;

        public XBusPipe(LocalSocket socket) {
            this.socket = socket;
            try {
                this.min = new MessageReader(socket.getInputStream());
                this.mout = new MessageWriter(socket.getOutputStream());
                new Thread(this, "pipe:" + name).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Message read() throws IOException {
            Message msg = min.read();
            return msg;
        }

        void write(Message msg) {
            mout.write(msg);
        }

        void stopRunning() {
            running.set(false);
        }

        @Override
        public void run() {
            while (running.get()) {
                Message msg = null;
                try {
                    msg = read();
                    String dest = msg.getDest();
                    XBusPipe pipe = mRouter.pipes.get(dest);
                    synchronized (mRouter.inqueue) {
                        mRouter.inqueue.putLast(msg, new WeakReference<XBusPipe>(pipe));
                        mRouter.inqueue.notifyAll();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
