package com.bilibili.xbus.proxy;

import java.io.Serializable;

/**
 * RemoteObject
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class RemoteObject implements Serializable{

    private String className;

    public RemoteObject(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }
}
