/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Pair;

import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;
import com.bilibili.xbus.utils.MagicMap;
import com.bilibili.xbus.utils.XBusLog;

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
        mDispatcher = new Dispatcher();
    }

    static String getName(Context context) {
        return getAddress(context);
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
        mRouter.start();
        mDispatcher.start();
        super.start();
    }

    @Override
    public void run() {
        if (XBus.DEBUG) {
            Debug.waitForDebugger();
        }

        LocalServerSocket lss = null;
        try {
            lss = new LocalServerSocket(getAddress(mContext));
            if (XBusLog.ENABLE) {
                XBusLog.d("XBus daemon is running");
            }

            while (mIsRunning.get()) {
                LocalSocket ls = lss.accept();
                ls.setSoTimeout(XBus.DEFAULT_SO_TIMEOUT);

                if (XBusLog.ENABLE) {
                    XBusLog.d("accept socket: " + ls);
                }

                if (mXBusAuth.auth(ls)) {
                    new XBusConnection(ls).start();
                } else {
                    ls.close();
                }
            }
        } catch (IOException e) {
            if (XBusLog.ENABLE) {
                XBusLog.printStackTrace(e);
            }
        } finally {
            try {
                if (lss != null) {
                    lss.close();
                }
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
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
                Pair<Message, List<WeakReference<XBusConnection>>> pair = mRouter.pollOut();

                Message msg = pair.first;
                List<WeakReference<XBusConnection>> wcs = pair.second;

                if (wcs != null) {
                    for (WeakReference<XBusConnection> wp : wcs) {
                        XBusConnection conn = wp.get();

                        if (conn == null) {
                            continue;
                        }

                        conn.write(msg);
                    }
                }
            }
        }
    }

    class XBusRouter extends Thread {

        private final Map<String, XBusConnection> mConns = new HashMap<>();
        private final MagicMap<Message, WeakReference<XBusConnection>> mInQueue = new MagicMap<>();
        private final MagicMap<Message, WeakReference<XBusConnection>> mOutQueue = new MagicMap<>();

        void addConnection(XBusConnection conn) {
            synchronized (mConns) {
                mConns.put(conn.name, conn);
            }
        }

        void removeConnection(XBusConnection conn) {
            synchronized (mConns) {
                mConns.remove(conn.name);
            }
        }

        void offerIn(Message msg, XBusConnection conn) {
            synchronized (mInQueue) {
                mInQueue.putLast(msg, new WeakReference<>(conn));
                mInQueue.notifyAll();
            }
        }

        private void offerOut(Message msg, XBusConnection conn) {
            synchronized (mOutQueue) {
                mOutQueue.putLast(msg, new WeakReference<>(conn));
                mOutQueue.notifyAll();
            }
        }

        @WorkerThread
        @NonNull
        private Pair<Message, List<WeakReference<XBusConnection>>> pollIn() {
            Message msg;
            List<WeakReference<XBusConnection>> wcs;
            synchronized (mInQueue) {
                try {
                    while (mInQueue.size() == 0) {
                        mInQueue.wait();
                    }
                } catch (InterruptedException ignored) {
                }

                msg = mInQueue.head();
                wcs = mInQueue.remove(msg);
            }

            return new Pair<>(msg, wcs);
        }

        @WorkerThread
        @NonNull
        Pair<Message, List<WeakReference<XBusConnection>>> pollOut() {
            Message msg;
            List<WeakReference<XBusConnection>> wcs;

            synchronized (mOutQueue) {
                try {
                    while (mOutQueue.size() == 0) {
                        mOutQueue.wait();
                    }
                } catch (InterruptedException ignored) {
                }

                msg = mOutQueue.head();
                wcs = mOutQueue.remove(msg);
            }

            return new Pair<>(msg, wcs);
        }

        @Override
        public void run() {
            while (mIsRunning.get()) {
                Pair<Message, List<WeakReference<XBusConnection>>> pair = pollIn();
                Message msg = pair.first;
                List<WeakReference<XBusConnection>> wcs = pair.second;

                if (wcs == null) {
                    continue;
                }

                for (WeakReference<XBusConnection> wp : wcs) {
                    XBusConnection conn = wp.get();

                    if (conn == null) {
                        continue;
                    }

                    offerOut(msg, conn);
                }
            }
        }
    }

    private AtomicBoolean mIsRunning = new AtomicBoolean(false);

    class XBusConnection extends Thread {

        String name;
        LocalSocket socket;
        private MessageReader min;
        private MessageWriter mout;

        public XBusConnection(LocalSocket socket) {
            super("XBusConnection");
            this.socket = socket;
            try {
                this.min = new MessageReader(XBusDaemon.getName(mContext), socket.getInputStream());
                this.mout = new MessageWriter(XBusDaemon.getName(mContext), socket.getOutputStream());
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }
            }
        }

        private boolean handshake() throws IOException {
            Message msg = new MethodCall(XBusDaemon.getName(mContext), "", "getName");
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
            return false;
        }

        void write(Message msg) {
            try {
                if (mout != null) {
                    mout.write(msg);
                }
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                close();
            }
        }

        private void close() {
            XBus.closeQuietly(mout);
            XBus.closeQuietly(min);
            try {
                socket.close();
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }
            }

            mRouter.removeConnection(this);
        }

        @Override
        public void run() {
            try {
                if (handshake()) {
                    if (XBusLog.ENABLE) {
                        XBusLog.d("XBusConnection " + name + "handshake success");
                    }

                    Message msg;
                    while (mIsRunning.get()) {
                        msg = min.read();
                        String dest = msg.getDest();
                        XBusConnection conn = mRouter.mConns.get(dest);
                        mRouter.offerIn(msg, conn);
                    }
                }
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }
            }

            close();
        }
    }
}
