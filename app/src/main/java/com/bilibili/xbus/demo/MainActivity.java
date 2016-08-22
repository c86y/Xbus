package com.bilibili.xbus.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bilibili.xbus.CallHandler;
import com.bilibili.xbus.Connection;
import com.bilibili.xbus.XBus;
import com.bilibili.xbus.XBusHost;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.proxy.RemoteCallHandler;
import com.bilibili.xbus.proxy.RemoteInvocation;

public class MainActivity extends AppCompatActivity implements CallHandler {

    private XBus mBus;
    private RemoteCallHandler mRemoteCallHandler;
    private TestEcho mTestEcho;
    private TestGetUserInfo mTestGetUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        XBusHost.init(this); // init XBusHost service
        startTestService();

        mRemoteCallHandler = new RemoteCallHandler("test");
        mBus = new XBus(this, "main", this).registerCallHandler(mRemoteCallHandler);

        mTestEcho = RemoteInvocation.getProxy(TestEcho.class, mRemoteCallHandler);
        mTestGetUser = RemoteInvocation.getProxy(TestGetUserInfo.class, mRemoteCallHandler);

        Button btn1 = (Button) findViewById(R.id.btn1);
        if (btn1 != null) {
            btn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String msg = mTestEcho.talk("hello");
                    toast("return : " + String.valueOf(msg));
                }
            });
        }

        Button btn2 = (Button) findViewById(R.id.btn2);
        if (btn2 != null) {
            btn2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTestEcho.callBackTalk("hello");

                }
            });
        }

        Button btn3 = (Button) findViewById(R.id.btn3);
        if (btn3 != null) {
            btn3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mTestGetUser.getUser();
                }
            });
        }

        mRemoteCallHandler.registerRemoteCallBack(new RemoteCallHandler.RemoteCallBack() {
            @Override
            public void callBack(String action, Object object) {
                switch (action) {
                    case TestEcho.CallBack.METHOD_TALK:
                        toast("return : " + String.valueOf(object));
                        break;
                    case TestGetUserInfo.CallBack.METHOD_GET_USER:
                        toast("return : " + String.valueOf(object));
                        break;
                    default:
                        break;
                }
            }
        });

        mBus.connect();
    }

    private void startTestService() {
        Intent intent = new Intent(this, TestService.class);
        startService(intent);
    }

    protected void toast(String msg) {
        if (TextUtils.isEmpty(msg)) return;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handleMessage clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // XBusHost.close(this);
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
