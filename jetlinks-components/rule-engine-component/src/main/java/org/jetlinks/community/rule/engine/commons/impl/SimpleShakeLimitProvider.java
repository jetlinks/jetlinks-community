package org.jetlinks.community.rule.engine.commons.impl;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.ShakeLimitProvider;
import org.jetlinks.community.rule.engine.commons.ShakeLimitResult;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;

@Slf4j
public class SimpleShakeLimitProvider implements ShakeLimitProvider {

    public static final ShakeLimitProvider GLOBAL = new SimpleShakeLimitProvider();

    public static final String PROVIDER = "simple";

    @Override
    public String provider() {
        return PROVIDER;
    }

    protected <T> Flux<T> wrapSource(String sourceKey, Flux<T> source) {
        return source;
    }

    @Override
    public <T> Flux<ShakeLimitResult<T>> shakeLimit(String sourceKey,
                                                    Flux<GroupedFlux<String, T>> grouped,
                                                    ShakeLimit limit) {
        int thresholdNumber = limit.getThreshold();
        boolean isAlarmFirst = limit.isAlarmFirst();
        Duration windowSpan = Duration.ofSeconds(limit.getTime());
        return grouped
            .flatMap(group -> {
                String groupKey = group.key();
                String key = sourceKey + ":" + groupKey;
                return Flux
                    .defer(() -> this
                        //使用timeout,当2倍窗口时间没有收到数据时,则结束分组.释放内存.
                        .wrapSource(key, group.timeout(windowSpan.plus(windowSpan), Mono.empty())))
                    //按时间窗口分组
                    .window(windowSpan)
                    .flatMap(source -> this
                        .handleWindow(key,
                                      groupKey,
                                      windowSpan,
                                      source,
                                      thresholdNumber,
                                      isAlarmFirst))
                    .onErrorResume(err -> {
                        log.warn("shake limit [{}] error", key, err);
                        return Mono.empty();
                    });
            }, Integer.MAX_VALUE);
    }

    protected <T> Mono<ShakeLimitResult<T>> handleWindow(String key,
                                                         String groupKey,
                                                         Duration duration,
                                                         Flux<T> source,
                                                         long thresholdNumber,
                                                         boolean isAlarmFirst) {
        //给数据打上索引,索引号就是告警次数
        return source
            .index((index, data) -> Tuples.of(index + 1, data))
            .switchOnFirst((e, flux) -> {
                if (e.hasValue()) {
                    @SuppressWarnings("all")
                    T ele = e.get().getT2();
                    return flux.map(tp2 -> Tuples.of(tp2.getT1(), tp2.getT2(), ele));
                }
                return flux.then(Mono.empty());
            })
            //超过阈值告警时
            .filter(tp -> tp.getT1() >= thresholdNumber)
            .as(flux -> isAlarmFirst ? flux.take(1) : flux.takeLast(1))//取第一个或者最后一个
            .map(tp3 -> {
                T next = isAlarmFirst ? tp3.getT3() : tp3.getT2();
                return new ShakeLimitResult<>(groupKey, tp3.getT1(), next);
            })
            .singleOrEmpty();
    }
}
