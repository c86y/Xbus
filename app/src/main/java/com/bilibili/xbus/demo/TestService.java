package com.bilibili.xbus.demo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bilibili.xbus.XBus;
import com.bilibili.xbus.Connection;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;

/**
 * TestService
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class TestService extends Service implements XBus.CallHandler {

    private Handler mHandler;
    private XBus mBus;
    private Connection mCoon;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mBus = new XBus(this, "test");

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBus.connect(TestService.this);
            }
        }, 1000);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConnect(Connection conn) {
        mCoon = conn;
    }

    @Override
    public void handleMessage(final Message msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Connection conn = mCoon;
                if (conn != null) {
                    conn.send(new MethodReturn(mBus.getPath(), msg.getSource(), msg.getSerial(), msg.getAction()));
                }
            }
        });
    }

    @Override
    public void onDisconnect() {
        mCoon = null;
    }
}
