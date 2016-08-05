package com.bilibili.xbus.demo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.bilibili.xbus.XBus;
import com.bilibili.xbus.XBusException;
import com.bilibili.xbus.XBusService;
import com.bilibili.xbus.message.Message;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements XBus.CallbackHandler{

    private XBus mXBus;
    private FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mXBus = new XBus(this);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        if (mFab != null) {
            mFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Map<Byte, Object> headers = new HashMap<>();
                    headers.put(Message.HeaderField.SOURCE, mXBus.getName());
                    headers.put(Message.HeaderField.DEST, mXBus.getName());
                    Message msg = new Message(Message.MessageType.MESSAGE_CALL, headers, null);
                    mXBus.send(msg);
                }
            });
        }
        startXbusDameon();

        try {
            Debug.waitForDebugger();
            mXBus.connect(MainActivity.this);
        } catch (XBusException e) {
            e.printStackTrace();
        }
    }

    private void startXbusDameon() {
        Intent intent = new Intent(this, XBusService.class);
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
        if (mXBus != null) {
            try {
                mXBus.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handle(Message msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(mFab, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }
}
