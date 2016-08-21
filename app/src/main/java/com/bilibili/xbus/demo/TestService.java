package com.bilibili.xbus.demo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.bilibili.xbus.CallHandler;
import com.bilibili.xbus.Connection;
import com.bilibili.xbus.XBus;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.proxy.RemoteCallHandler;

/**
 * TestService
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class TestService extends Service implements CallHandler{

    private XBus mBus;
    private RemoteCallHandler mRemoteCallHandler;
    private TestInterface mTestInterface = new TestInterface() {
        @Override
        public String talk(String str) {
            return "echo " + str;
        }

        @Override
        public String callBackTalk(String str, CallBack callBack) {
            return "echo " + str;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteCallHandler = new RemoteCallHandler("main");
        mBus = new XBus(this, "test", this).registerCallHandler(mRemoteCallHandler);
        mRemoteCallHandler.registerObject(TestInterface.class, mTestInterface);

        mBus.connect();
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
    public void onConnected(Connection conn) {

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void handleMessage(Message msg) {

    }
}
