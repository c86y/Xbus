package com.bilibili.xbus.demo;

import android.app.Application;

import com.antfortune.freeline.FreelineCore;

/**
 * @author kaede
 * @version date 16/8/19
 */
public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FreelineCore.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
