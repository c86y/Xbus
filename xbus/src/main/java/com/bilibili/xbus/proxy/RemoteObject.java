package com.bilibili.xbus.proxy;

import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * RemoteObject
 *
 * @author chengyuan
 * @data 16/8/12.
 */
public class RemoteObject implements Serializable {

    private final String className;

    public RemoteObject(@NonNull String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RemoteObject that = (RemoteObject) o;

        return className.equals(that.className);

    }

    @Override
    public int hashCode() {
        return className.hashCode();
    }
}
