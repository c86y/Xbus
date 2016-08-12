package com.bilibili.xbus.demo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bilibili.xbus.XBusClient;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodReturn;

/**
 * TestService
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class TestService extends Service implements XBusClient.CallbackHandler{

    private Handler mHandler;
    private XBusClient mBus;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mBus = new XBusClient(this, "test");

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
    public void handle(final Message msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mBus.send(new MethodReturn(mBus.getPath(), msg.getSource(), msg.getSerial(), msg.getAction()));
            }
        });
    }
}
