package com.yhp.subscriber;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

public class SampleSubscriber<T> extends BaseSubscriber<T> {
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        System.out.println("Subscribed");
        //向上游发送背压请求，请求一个元素
        request(1);
    }

    @Override
    protected void hookOnNext(T value) {
        System.out.println(value);
        request(1);
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        System.out.println("Error: "+throwable);
        request(1);
    }
}
