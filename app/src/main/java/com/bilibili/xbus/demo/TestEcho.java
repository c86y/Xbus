package com.bilibili.xbus.demo;

import com.bilibili.xbus.annotation.CallBackAction;

/**
 * TestInterface
 *
 * @author chengyuan
 * @data 16/8/16.
 */
public interface TestEcho {

    interface CallBack {
        String METHOD_TALK = "talk";
    }

    String talk(String str);

    @CallBackAction(CallBack.METHOD_TALK)
    String callBackTalk(String str);
}
