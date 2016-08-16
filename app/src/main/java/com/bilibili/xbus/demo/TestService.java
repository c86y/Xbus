package com.bilibili.xbus.demo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bilibili.xbus.XBus;
import com.bilibili.xbus.proxy.RemoteCallHandler;

/**
 * TestService
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class TestService extends Service {

    private Handler mHandler;
    private XBus mBus;
    private RemoteCallHandler mRemoteCallHandler;
    private TestInterface mTestInterface = new TestInterface() {
        @Override
        public String talk(String str) {
            return "echo " + str;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mBus = new XBus(this, "test");
        mRemoteCallHandler = new RemoteCallHandler("main");
        mRemoteCallHandler.registerObject(TestInterface.class, mTestInterface);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBus.connect(mRemoteCallHandler);
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
}
