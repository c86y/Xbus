package com.bilibili.xbus.utils;

import java.io.Serializable;

/**
 * Watch Dog
 * Created by Kaede on 16/8/17.
 */
public class StopWatch implements Serializable {
    private long startTime;
    private long splitTime;
    private long endTime;
    private StringBuilder stringBuilder;

    // {name → [tag1 = 100 ms] → [tag1 = 100 ms] ：all = 200 ms}
    public StopWatch() {
    }

    public StopWatch start(String name) {
        startTime = System.nanoTime();
        splitTime = System.nanoTime();
        endTime = System.nanoTime();
        stringBuilder = new StringBuilder(String.format("{%s", name));
        return this;
    }

    public StopWatch split() {
        return split(" ");
    }

    public StopWatch split(String tag) {
        endTime = System.nanoTime();
        String interval = getInterval(splitTime, endTime);
        stringBuilder.append(String.format(" → [%s = %s ms]", tag, interval));
        splitTime = endTime;
        return this;
    }

    public String end() {
        return end(" ");
    }

    public String end(String tag) {
        split(tag);
        endTime = System.nanoTime();
        String interval = getInterval(startTime, endTime);
        stringBuilder.append(String.format(" ：all = %s ms}", interval));
        return stringBuilder.toString();
    }

    private String getInterval(long start, long end) {
        long l = end - start;
        return String.valueOf(l / 1000000f);
    }
}
