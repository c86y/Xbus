package com.bilibili.xbus.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
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
import com.bilibili.xbus.XBusService;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.proxy.RemoteCallHandler;
import com.bilibili.xbus.proxy.RemoteInvocation;

public class MainActivity extends AppCompatActivity implements CallHandler {

    private XBus mBus;
    private Button btn1;
    private RemoteCallHandler mRemoteCallHandler;
    private TestInterface mTestInterface;

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
        mTestInterface = RemoteInvocation.getProxy(TestInterface.class, mRemoteCallHandler);

        btn1 = (Button) findViewById(R.id.btn1);
        if (btn1 != null) {
            btn1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String echo = mTestInterface.talk("hello world");
                    toast("Read msg: " + echo);
                }
            });
        }

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
        XBusService.stopService(this);
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
