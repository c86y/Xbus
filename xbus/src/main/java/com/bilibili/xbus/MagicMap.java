/*
 * Copyright (c) 2015-2016 BiliBili Inc.
 */

package com.bilibili.xbus;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * MagicMap
 *
 * @author chengyuan
 * @data 16/8/4.
 */
public class MagicMap<A, B> {
    private Map<A, LinkedList<B>> m;
    private LinkedList<A> q;

    public MagicMap() {
        m = new HashMap<A, LinkedList<B>>();
        q = new LinkedList<A>();
    }

    public A head() {
        return q.getFirst();
    }

    public void putFirst(A a, B b) {
        if (m.containsKey(a))
            m.get(a).add(b);
        else {
            LinkedList<B> l = new LinkedList<B>();
            l.add(b);
            m.put(a, l);
        }
        q.addFirst(a);
    }

    public void putLast(A a, B b) {
        if (m.containsKey(a))
            m.get(a).add(b);
        else {
            LinkedList<B> l = new LinkedList<B>();
            l.add(b);
            m.put(a, l);
        }
        q.addLast(a);
    }

    public List<B> remove(A a) {
        q.remove(a);
        return m.remove(a);
    }

    public int size() {
        return q.size();
    }
}
