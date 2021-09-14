package com.yhp.intf;

import java.util.List;

public interface MyEventListener<T> {

    //监听chunk数据准备好了
    void onDataChunk(List<T> chunk);

    //监听处理完成
    void processComplete();
}
