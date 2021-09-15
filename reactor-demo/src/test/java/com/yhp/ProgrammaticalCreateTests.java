package com.yhp;

import com.yhp.channel.MyServerSocketChannel;
import com.yhp.intf.MyEventListener;
import com.yhp.processor.MyEventProcessor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author haopeng.yuan@haiziwang.com
 * @version V1.0
 * Copyright 2019 Kidswant Children Products Co., Ltd.
 * @Title:
 * @Description:
 * @date
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class ProgrammaticalCreateTests {

    /**
     * Generate 同步、逐个产生值。
     * sink是一个同步sink，sink的next()在每次回调时最多只能被调用一次。
     * 最常用的方式是记录一个状态值state，sink可以基于state产生下一个元素。
     * Supplier用于初始化state。
     * 本例中，原生类型、包装类型、String等是final类型。当state是可变类型时，sink返回的是同一个实例。
     */
    @Test
    public void generateTest() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(System.out::println);
    }

    /**
     * sink complete之后，会调用Consumer。
     * Consumer中可以用来在最后关闭连接或完成其他清理任务。
     */
    @Test
    public void generateConsumerTest() {
        Flux<String> flux = Flux.generate(
                AtomicLong::new,
                (state, sink) -> {
                    long i = state.getAndIncrement();
                    sink.next("3 x " + i + " = " + 3 * i);
                    if (i == 10) sink.complete();
                    return state;
                }, (state) -> System.out.println("state: " + state));
        flux.subscribe(System.out::println);
    }


    /**
     * create方法生成的flux既可以是同步的，也可以是异步的，并且每次可以发出多个元素。
     * 与generate相比，create不需要state，并且可以在回调中触发多个事件。
     * create可以将现有的API转为响应式API，如监听器的异步方法。
     * 与push方法不同的是，create可以多线程生成元素，而push只用一个线程生成元素
     */
    @Test
    public void createTest() {
        //自定义事件处理器
        MyEventProcessor<String> myEventProcessor = new MyEventProcessor<>();
        //如果需要单线程生成Flux，可以使用Flux.push
        Flux<String> bridge = Flux.create(sink -> myEventProcessor.register(new MyEventListener<String>() {
            @Override
            public void onDataChunk(List<String> chunk) {
                System.out.println("chunk data has been prepared.");
                for (String s : chunk) {
                    //将chunk中的数据转为Flux的元素，这里Flux中的元素是异步生成的。
                    sink.next(s);
                }
            }
            @Override
            public void processComplete() {
                System.out.println("data has been processed.");
                sink.complete();
            }
            //溢出策略，控制下游的背压请求，默认BUFFER为缓存住下游尚未处理的元素，可能导致OOM
        }), FluxSink.OverflowStrategy.BUFFER);
        bridge.subscribe(e -> System.out.println("get data: "+e));
        myEventProcessor.processEvent();
    }

    /**
     * create方法可以主动从数据源拉取数据。
     */
    @Test
    public void pullTest() {
        //自定义事件处理器
        MyEventProcessor<String> myEventProcessor = new MyEventProcessor<>();

        Flux<String> bridge = Flux.create(sink -> {
            myEventProcessor.register(new MyEventListener<String>() {
                @Override
                public void onDataChunk(List<String> chunk) {
                    System.out.println("chunk data has been prepared.");
                    for (String s : chunk) {
                        //监听processor产生数据，推送到Flux中
                        sink.next(s);
                    }
                }
                @Override
                public void processComplete() {
                    System.out.println("data has been processed.");
                    sink.complete();
                }
            });

            //从processor中拉取数据，放入Flux中
            sink.onRequest(n -> {
                List<String> messages = myEventProcessor.request(n);
                for (String message : messages) {
                    sink.next(message);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
        bridge.subscribe(e -> System.out.println("get data: "+e));
        myEventProcessor.processEvent();
    }

    /**
     * onDispose: Flux完成、有错误、或被取消时执行清理
     * onCancel: 取消时执行，先于onDispose执行
     */
    @Test
    public void cleanUpTest() {
        MyServerSocketChannel channel = new MyServerSocketChannel();

        Flux<String> flux = Flux.create(sink -> {
            sink.onRequest(n -> {
                List<String> poll = channel.poll(n);
                for (String s : poll) {
                    sink.next(s);
                }
            }).onCancel(channel::cancel)
              .onDispose(channel::close);
        });

        Disposable dispose = flux.subscribe(e -> {
            System.out.println("get channel data: " + e);
            try {
                Thread.sleep(2000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        //停止向Flux中推数据
        dispose.dispose();
    }


    /**
     * Flux对象的handle方法可以对Flux中的元素进行处理。
     */
    @Test
    public void handleTest() {
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                .handle((i, sink) -> {
                    String letter = alphabet(i);
                    //过滤掉Flux中的null元素
                    if (letter != null) {
                        sink.next(letter);
                    }
                });

        alphabet.subscribe(System.out::println);
    }

    public String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }
}
