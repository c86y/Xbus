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

import com.bilibili.xbus.XBusClient;
import com.bilibili.xbus.XBusService;
import com.bilibili.xbus.message.Message;
import com.bilibili.xbus.message.MethodCall;

public class MainActivity extends AppCompatActivity implements XBusClient.CallbackHandler {

    private XBusClient mBus;
    private FloatingActionButton mFab;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        startXBus();
        startTestService();

        mBus = new XBusClient(this, "main");
        mHandler = new Handler();

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Message msg = new MethodCall(mBus.getPath(), "test", "hello");
                    mBus.send(msg);
                }
            });
        }

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBus.connect(MainActivity.this);
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
        // automatically handle clicks on the Home/Up button, so long
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
        if (mBus != null) {
            mBus.disconnect();
        }
    }

    @Override
    public void handle(final Message msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(mFab, "Read msg: " + msg, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }
}
