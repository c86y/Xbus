package com.bilibili.xbus.demo;

import java.io.Serializable;

/**
 * @author kaede
 * @version date 16/8/22
 */
public class User implements Serializable {
    public String name;
    public long id;

    public User(String name, long id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
