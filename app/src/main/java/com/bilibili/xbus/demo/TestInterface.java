package com.bilibili.xbus.demo;

import java.io.Serializable;

/**
 * TestInterface
 *
 * @author chengyuan
 * @data 16/8/16.
 */
public interface TestInterface {

    interface CallBack extends Serializable {
        void onGetMsg(String msg);
    }

    String talk(String str);

    String callBackTalk(String str, CallBack callBack);
}
