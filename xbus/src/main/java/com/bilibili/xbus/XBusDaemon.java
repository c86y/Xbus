/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;

import java.io.IOException;
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
    private XBusRouter mRouter;
    private Dispatcher mDispatcher;

    private XBusAuth mXBusAuth = new FastXBusAuth();

    XBusDaemon(Context context) {
        super(TAG);
        mContext = context.getApplicationContext();
        mRouter = new XBusRouter();
        mRouter.start();
        mDispatcher = new Dispatcher();
        mDispatcher.start();
    }

    static String getAddress(Context context) {
        return context.getPackageName() + SOCKET_NAME;
    }

    void stopRunning() {
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
            lss = new LocalServerSocket(getAddress(mContext));
            if (XBusLog.DEBUG) {
                XBusLog.d("XBus daemon is running");
            }

            while (mIsRunning.get()) {
                LocalSocket ls = lss.accept();

                if (XBusLog.DEBUG) {
                    XBusLog.d("accept socket: " + ls);
                }

                if (mXBusAuth.auth(ls)) {
                    new XBusPipe(ls);
                } else {
                    ls.close();
                }
            }
        } catch (IOException e) {
            if (XBusLog.DEBUG) {
                XBusLog.printStackTrace(e);
            }
        } finally {
            try {
                if (lss != null) {
                    lss.close();
                }
            } catch (IOException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }
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
            while (mIsRunning.get()) {
                Message msg;
                List<WeakReference<XBusPipe>> wps;
                synchronized (mRouter.outqueue) {
                    try {
                        while (mRouter.outqueue.size() == 0) {
                            mRouter.outqueue.wait();
                        }
                    } catch (InterruptedException ignored) {
                    }

                    msg = mRouter.outqueue.head();
                    wps = mRouter.outqueue.remove(msg);
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
    }

    class XBusRouter extends Thread {

        private final Map<String, XBusPipe> pipes = new HashMap<>();
        final MagicMap<Message, WeakReference<XBusPipe>> inqueue = new MagicMap<>();
        final MagicMap<Message, WeakReference<XBusPipe>> outqueue = new MagicMap<>();

        void addConnection(XBusPipe pipe) {
            synchronized (pipes) {
                pipes.put(pipe.name, pipe);
            }
        }

        void removeConnection(XBusPipe pipe) {
            synchronized (pipes) {
                pipes.remove(pipe.name);
            }
        }

        @Override
        public void run() {
            while (mIsRunning.get()) {
                Message msg;
                List<WeakReference<XBusPipe>> wps;
                synchronized (mRouter.inqueue) {
                    try {
                        while (mRouter.inqueue.size() == 0) {
                            mRouter.inqueue.wait();
                        }
                    } catch (InterruptedException ignored) {
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

                        synchronized (mRouter.outqueue) {
                            mRouter.outqueue.putLast(msg, new WeakReference<>(pipe));
                            mRouter.outqueue.notifyAll();
                        }
                    }
                }
            }
        }
    }

    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    class XBusPipe implements Runnable {

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
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }
            }
        }

        private boolean handshake() {
            try {
                Message msg = new MethodCall(getAddress(mContext), "", "getName", null);
                mout.write(msg);

                msg = min.read();
                if (msg != null) {
                    Object[] args = msg.getArgs();

                    if (args != null && args.length > 0) {
                        this.name = (String) args[0];
                        mRouter.addConnection(this);
                        return true;
                    }
                }
            } catch (IOException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }

                close();
            }
            return false;
        }

        void write(Message msg) {
            try {
                mout.write(msg);
            } catch (IOException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }

                close();
            }
        }

        private void close() {
            XBus.closeQuietly(mout);
            XBus.closeQuietly(min);

            mRouter.removeConnection(this);
        }


        @Override
        public void run() {
            if (!handshake()) {
                return;
            }

            if (XBusLog.DEBUG) {
                XBusLog.d("XBusPipe " + name + "run");
            }

            Message msg;
            try {
                while (mIsRunning.get()) {
                    msg = min.read();
                    String dest = msg.getDest();
                    XBusPipe pipe = mRouter.pipes.get(dest);
                    synchronized (mRouter.inqueue) {
                        mRouter.inqueue.putLast(msg, new WeakReference<>(pipe));
                        mRouter.inqueue.notifyAll();
                    }
                }
            } catch (IOException e) {
                if (XBusLog.DEBUG) {
                    XBusLog.printStackTrace(e);
                }
            }

            close();
        }
    }
}
