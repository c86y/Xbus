package com.bilibili.xbus.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bilibili.xbus.XBus;
import com.bilibili.xbus.Connection;
import com.bilibili.xbus.XBusService;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;
import com.bilibili.xbus.proxy.RemoteCallHandler;
import com.bilibili.xbus.proxy.RemoteInvocation;
import com.bilibili.xbus.proxy.RemoteObject;

public class MainActivity extends AppCompatActivity {

    private XBus mBus;
    private FloatingActionButton mFab;
    private Handler mHandler;
    private RemoteCallHandler mRemoteCallHandler;
    private TestInterface mTestInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startXBus();
        startTestService();

        mBus = new XBus(this, "main");
        mHandler = new Handler();
        mRemoteCallHandler = new RemoteCallHandler("test");
        mTestInterface = RemoteInvocation.getProxy(TestInterface.class, mRemoteCallHandler);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String echo = mTestInterface.talk("hello world");
                    Snackbar.make(mFab, "Read msg: " + echo, Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show();
                }
            });
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBus.connect(mRemoteCallHandler);
            }
        }, 1000);

    }

    private void startXBus() {
        Intent intent = new Intent(this, XBusService.class);
        startService(intent);
    }

    private void startTestService() {
        Intent intent = new Intent(this, TestService.class);
        startService(intent);
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
    }
}
