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

import com.bilibili.xbus.message.ErrorCode;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;
import com.bilibili.xbus.utils.MagicMap;
import com.bilibili.xbus.utils.XBusLog;
import com.bilibili.xbus.utils.XBusUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XBusHost
 *
 * @author chengyuan
 */
public class XBusHost extends Thread {

    private static final String TAG = "XBusHost";

    public static void init(Context context) {
        XBusService.startService(context);
    }

    public static void close(Context context) {
        XBusService.stopService(context);
    }

    private final String mHostPath;
    private final XBusAuth mXBusAuth;

    private Context mContext;
    private XBusRouter mRouter;
    private Dispatcher mDispatcher;
    private AtomicBoolean mRunning = new AtomicBoolean(false);

    XBusHost(Context context) {
        super(TAG);
        mContext = context.getApplicationContext();
        mHostPath = XBusUtils.getHostPath(mContext);
        mXBusAuth = new FastAuth();
        mRouter = new XBusRouter();
        mDispatcher = new Dispatcher();
        setDaemon(true);
    }

    void stopRunning() {
        if (XBusLog.ENABLE) {
            XBusLog.d("XBusHost is stopped");
        }
        mRunning.set(false);
    }

    @Override
    public synchronized void start() {
        mRunning.set(true);
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
            lss = new LocalServerSocket(XBusUtils.getHostAddress(mContext));
            if (XBusLog.ENABLE) {
                XBusLog.d("XBus Host is running");
            }

            while (mRunning.get()) {
                LocalSocket ls = lss.accept();
                ls.setSoTimeout(XBus.DEFAULT_SO_TIMEOUT);

                if (XBusLog.ENABLE) {
                    XBusLog.d("accept socket: " + ls);
                }

                XBusAuth.AuthResult result = mXBusAuth.auth(XBusAuth.MODE_SERVER, ls);
                if (result == null || !result.success) {
                    ls.close();
                    continue;
                }

                new HostConnection(ls).start();
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

        private static final String TAG = "Host#Dispatcher";

        Dispatcher() {
            super(TAG);
        }

        @Override
        public void run() {
            while (mRunning.get()) {
                Pair<Message, List<WeakReference<HostConnection>>> pair = mRouter.pollOut();

                Message msg = pair.first;
                List<WeakReference<HostConnection>> wcs = pair.second;

                if (wcs != null) {
                    for (WeakReference<HostConnection> wp : wcs) {
                        HostConnection conn = wp.get();

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

        private static final String TAG = "Host#Router";
        private final Map<String, HostConnection> mConns = new HashMap<>();
        private final MagicMap<Message, WeakReference<HostConnection>> mInQueue = new MagicMap<>();

        private final MagicMap<Message, WeakReference<HostConnection>> mOutQueue = new MagicMap<>();

        public XBusRouter() {
            super(TAG);
        }

        void addConnection(HostConnection conn) {
            synchronized (mConns) {
                mConns.put(conn.clientPath, conn);
            }
        }

        void removeConnection(HostConnection conn) {
            synchronized (mConns) {
                mConns.remove(conn.clientPath);
            }
        }

        void offerIn(Message msg, HostConnection conn) {
            synchronized (mInQueue) {
                mInQueue.putLast(msg, new WeakReference<>(conn));
                mInQueue.notifyAll();
            }
        }

        private void offerOut(Message msg, HostConnection conn) {
            synchronized (mOutQueue) {
                mOutQueue.putLast(msg, new WeakReference<>(conn));
                mOutQueue.notifyAll();
            }
        }

        @WorkerThread
        @NonNull
        private Pair<Message, List<WeakReference<HostConnection>>> pollIn() {
            Message msg;
            List<WeakReference<HostConnection>> wcs;
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
        Pair<Message, List<WeakReference<HostConnection>>> pollOut() {
            Message msg;
            List<WeakReference<HostConnection>> wcs;

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
            while (mRunning.get()) {
                Pair<Message, List<WeakReference<HostConnection>>> pair = pollIn();
                Message msg = pair.first;
                List<WeakReference<HostConnection>> wcs = pair.second;

                if (wcs == null) {
                    continue;
                }

                for (WeakReference<HostConnection> wp : wcs) {
                    HostConnection conn = wp.get();

                    if (conn == null) {
                        continue;
                    }

                    offerOut(msg, conn);
                }
            }
        }

    }

    class HostConnection extends Thread {

        public static final String TAG = "Host#Connection";

        String clientPath;
        LocalSocket socket;
        private MessageReader mIn;
        private MessageWriter mOut;
        private final XBusHandshake handshake;

        public HostConnection(LocalSocket socket) {
            super(TAG);
            this.socket = socket;
            handshake = XBusHandshakeImpl.instance(mContext);
            try {
                this.mIn = new MessageReader(mHostPath, socket.getInputStream());
                this.mOut = new MessageWriter(mHostPath, socket.getOutputStream());
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }
            }
        }

        void write(Message msg) {
            try {
                if (mOut != null) {
                    mOut.write(msg);
                }
            } catch (IOException e) {
                if (XBusLog.ENABLE) {
                    XBusLog.printStackTrace(e);
                }

                close();
            }
        }

        private void close() {
            XBusUtils.closeQuietly(mOut);
            XBusUtils.closeQuietly(mIn);
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
                clientPath = handshake.handshakeWithClient(mHostPath, mIn, mOut);
                mRouter.addConnection(this);

                if (XBusLog.ENABLE) {
                    XBusLog.d("connection " + clientPath + " handshake success");
                }

                Message msg;
                while (mRunning.get()) {
                    msg = mIn.read();
                    if (XBusLog.ENABLE) {
                        XBusLog.d("connection read msg from " + clientPath + ", msg = " + (msg == null ? "null" : msg));
                    }
                    if (msg == null) {
                        continue;
                    }

                    String dest = msg.getDest();
                    HostConnection conn = mRouter.mConns.get(dest);
                    if (conn == null) {
                        mOut.write(new MethodReturn(msg.getDest(), msg.getSource(), msg.getSerial(), ErrorCode.E_INVALID_ADDRESS));
                        if (XBusLog.ENABLE) {
                            XBusLog.d("connection read msg " + msg + " target address is invalid");
                        }
                        continue;
                    }

                    mRouter.offerIn(msg, conn);
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
