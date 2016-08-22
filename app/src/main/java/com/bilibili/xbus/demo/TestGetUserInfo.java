package com.bilibili.xbus.demo;

import com.bilibili.xbus.annotation.CallBackAction;

/**
 * @author kaede
 * @version date 16/8/22
 */
public interface TestGetUserInfo {
    interface CallBack {
        String METHOD_GET_USER = "get_users";
    }

    @CallBackAction(CallBack.METHOD_GET_USER)
    User getUser();
}
