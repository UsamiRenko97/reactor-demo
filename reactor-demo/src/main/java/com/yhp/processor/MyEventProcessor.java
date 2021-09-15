package com.yhp.processor;

import com.yhp.intf.MyEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author haopeng.yuan@haiziwang.com
 * @version V1.0
 * Copyright 2019 Kidswant Children Products Co., Ltd.
 * @Title:
 * @Description:
 * @date
 */

public class MyEventProcessor<T extends String> {

    private MyEventListener<T> listener;

    public void register(MyEventListener<T> listener) {
        this.listener = listener;
    }

    public void processEvent() {
        List<T> trunk = prepareData();
        listener.onDataChunk(trunk);
        processData();
        listener.processComplete();
    }

    private List<T> prepareData() {
        System.out.println("preparing data...");
        List<T> trunk = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            trunk.add((T) String.valueOf(i));
        }
        return trunk;
    }

    private void processData() {
        System.out.println("processing data...");
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
