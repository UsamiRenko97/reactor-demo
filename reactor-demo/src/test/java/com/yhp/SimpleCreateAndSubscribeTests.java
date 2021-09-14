package com.yhp;

import com.yhp.subscriber.SampleSubscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SimpleCreateAndSubscribeTests {

    @Test
    public void createDemo() {
        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");
        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> seq2 = Flux.fromIterable(iterable);

        Mono<String> noData = Mono.empty();
        Mono<String> data = Mono.just("foo");
        Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3);
    }

    @Test
    public void subscribeDemo() {
        //Q:Hot or Cold?
        Flux<Integer> ints = Flux.range(1, 4)
                .map(i -> {
                    if (i<=3) return i;
                    throw new RuntimeException("Go to 4");
                });

        SampleSubscriber<Integer> ss = new SampleSubscriber<>();

        ints.subscribe(System.out::println,
                error -> System.out.println("Error: "+error),
                () -> System.out.println("Done"),
                s -> ss.request(10));

        ints.subscribe(ss);
    }


}
