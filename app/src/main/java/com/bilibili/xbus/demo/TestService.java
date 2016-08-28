package com.bilibili.xbus.demo;

import android.app.Service;
import android.content.Intent;
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

    private TestEcho mTestEcho = new TestEcho() {
        @Override
        public String talk(String str) {
            return "echo " + str;
        }

        @Override
        public String callBackTalk(String str) {
            return "echo " + str;
        }
    };

    private TestGetUserInfo mTestGetUser = new TestGetUserInfo() {
        @Override
        public User getUser() {
            return new User("kaede", 10086);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mRemoteCallHandler = new RemoteCallHandler("main");
        mBus = new XBus(this, "test", this).registerCallHandler(mRemoteCallHandler);
        mRemoteCallHandler.registerObject(TestEcho.class, mTestEcho);
        mRemoteCallHandler.registerObject(TestGetUserInfo.class, mTestGetUser);

        mBus.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
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
