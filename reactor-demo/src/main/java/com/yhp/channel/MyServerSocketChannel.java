package com.yhp.channel;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author haopeng.yuan@haiziwang.com
 * @version V1.0
 * Copyright 2019 Kidswant Children Products Co., Ltd.
 * @Title:
 * @Description:
 * @date
 */

public class MyServerSocketChannel implements Closeable {
    private List<String> data = Arrays.asList("1", "2", "3", "4", "5");

    @Override
    public void close() {
        System.out.println("channel closed");
    }

    public List<String> poll(long n) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            list.add(data.get(i%data.size()));
        }
        return list;
    }

    public void cancel() {
        System.out.println("channel canceled");
    }
}
