package com.yhp;

import com.yhp.exception.CacheNotExistException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ErrorHandlerTests {

    /**
     * 单线程Flux中，subscribe时重写onError方法即可捕获异常。
     * 异常是一个停止信号，会导致Flux中断。
     * 下面代码等同于以下try块
     * try {
     *     for (int i = 1; i < 11; i++) {
     *         String v1 = doSomethingDangerous(i);
     *         String v2 = doSecondTransform(v1);
     *         System.out.println("RECEIVED " + v2);
     *     }
     * } catch (Throwable t) {
     *     System.err.println("CAUGHT " + t);
     * }
     */
    @Test
    public void onErrorTest() {
        Flux<String> s = Flux.range(1, 10)
                .map(this::doSomethingDangerous)
                .map(this::doSecondTransform);
        s.subscribe(v-> System.out.println("received: "+v),
                e-> System.err.println("caught: "+e));
    }

    private String doSomethingDangerous(int i) {
        if (i>7) throw new RuntimeException("number over limit: "+i);
        return String.valueOf(i);
    }

    private String doSecondTransform(String s) {
        return "number " + s;
    }


    /**
     * 异常时返回一个静态缺省值
     */
    @Test
    public void onErrorReturnTest() {
        Flux<String> s = Flux.range(1, 10)
                .map(this::doSomethingDangerous)
                .map(this::doSecondTransform)
                .onErrorReturn("caught big number");
        s.subscribe(v-> System.out.println("received: "+v),
                e-> System.err.println("caught: "+e));
    }

    /**
     * 没看懂onErrorResume里怎么获取当前元素
     */
    @Test
    public void onErrorResumeTest() {
        Flux<String> flux = Flux.range(1, 10)
                .map(this::queryFromCache)
                .onErrorResume(e -> {
                    if (e instanceof CacheNotExistException) {
                        return Flux.just(queryFromDB());
                    }
                    return Flux.error(e);
                });
        flux.subscribe(v-> System.out.println("received: "+v),
                e-> System.err.println("caught: "+e));
    }

    private String queryFromCache(int i) {
        if (i<7) return "query from cache: "+i;
        else throw new CacheNotExistException("cache not exist: "+i);
    }

    private String queryFromDB() {
        return "query from database";
    }


    @Test
    public void disposableTest() {
        AtomicBoolean isDisposed = new AtomicBoolean(false);
        Disposable disposableInstance = new Disposable() {
            @Override
            public void dispose() {
                isDisposed.set(true);
            }

            @Override
            public String toString() {
                return "DISPOSABLE";
            }
        };
        Flux<String> flux = Flux.using(
                //第一个lambda生成资源
                () -> disposableInstance,
                //第二个lambda处理资源，返回Flux<T>
                disposable -> Flux.just(disposableInstance.toString()),
                //第三个lambda在上一个lambda中的Flux终止或取消时清理资源
                Disposable::dispose)
                //检查终止信号的类型
                .doFinally(type -> {
                    switch (type) {
                        case CANCEL:
                            System.out.println("flux canceled");
                            break;
                        case ON_COMPLETE:
                            System.out.println("flux completed");
                            break;
                        default:
                    }
                });
        flux.subscribe(System.out::println);
    }

    /**
     * retry开启的是另一个新Flux，上一个Flux已经抛出异常信号终止了。
     * elapsed记录了与上一个元素发出的时间间隔。
     * @throws InterruptedException
     */
    @Test
    public void retryTest() throws InterruptedException {
        Flux.interval(Duration.ofMillis(250))
                .map(i -> {
                    if (i<3) return "tick "+i;
                    throw new RuntimeException("boom");
                })
                .elapsed()
                //companion是一个Flux<Throwable>,重试循环：
                //1.每次出现错误，错误信号发给companion
                //2.如果companion发出元素，则触发重试
                //3.如果companion完成了，重试循环停止，并且原始序列也会完成
                //4.如果companion发出错误信号，重试循环停止，并且这个错误导致原始序列终止或完成。
                .retryWhen(companion-> {
                    //这里等于retry(3)
                    Flux<Throwable> take = companion.take(3);
                    take.subscribe(System.err::println);
                    return take;
                })
                .subscribe(System.out::println, System.err::println);
        Thread.sleep(6000);
    }
}
