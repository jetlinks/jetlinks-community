package org.jetlinks.community.rule.engine.commons;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * 抖动限制
 * <a href="https://github.com/jetlinks/jetlinks-community/issues/8">https://github.com/jetlinks/jetlinks-community/issues/8</a>
 *
 * @since 1.3
 */
@Getter
@Setter
public class ShakeLimit implements Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    @Schema(description = "是否开启防抖")
    private boolean enabled;

    //时间限制,单位时间内发生多次告警时,只算一次。单位:秒
    @Schema(description = "时间间隔(秒)")
    private int time;

    //触发阈值,单位时间内发生n次告警,只算一次。
    @Schema(description = "触发阈值(次)")
    private int threshold;

    //当发生第一次告警时就触发,为false时表示最后一次才触发(告警有延迟,但是可以统计出次数)
    @Schema(description = "是否第一次满足条件就触发")
    private boolean alarmFirst;

    /**
     * 利用窗口函数,将ReactorQL语句包装为支持抖动限制的SQL.
     * <p>
     * select * from ( sql )
     * group by
     * _window('1s') --时间窗口
     * ,trace() -- 跟踪分组内行号信息
     * ,take(-1) --取最后一条数据
     * having row.index >= 2"; -- 行号信息索引就是数据量
     *
     * @param sql 原始SQL
     * @return 防抖SQL
     */
    public String wrapReactorQl(@Nonnull String sql,
                                @Nullable String groupBy) {
        if (!enabled || time <= 0) {
            return sql;
        }
        int takes = Math.max(threshold, 1);

        return "select t.* from (" + sql + ") t" +
            " group by " + (StringUtils.hasText(groupBy) ? groupBy + "," : "") +
            "_window('" + time + "s')" + //时间窗口
            ",trace()" +    //跟踪分组后新的行信息,row.index为分组内的行号,row.elapsed为与上一行数据间隔时间(毫秒)
            ",take(" + (alarmFirst ? takes : -1) + ")" +
            " having row.index >= " + takes;

    }

    /**
     * 将流转换为支持抖动限制的流
     *
     * @param source         数据源
     * @param windowFunction 窗口函数
     * @param totalConsumer  总数接收器
     * @param <T>            数据类型
     * @return 新流
     * @deprecated {@link ShakeLimitProvider#shakeLimit(String, Flux, ShakeLimit)}
     */
    public <T> Flux<T> transfer(Flux<T> source,
                                BiFunction<Duration, Flux<T>, Flux<Flux<T>>> windowFunction,
                                BiConsumer<T, Long> totalConsumer) {
        if (!enabled || time <= 0) {
            return source;
        }
        int thresholdNumber = getThreshold();
        Duration windowTime = Duration.ofSeconds(getTime());

        return windowFunction
            .apply(windowTime, source)
            //处理每一组数据
            .flatMap(group -> group
                //给数据打上索引,索引号就是告警次数
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
                .as(flux -> isAlarmFirst() ? flux.take(1) : flux.takeLast(1))//取第一个或者最后一个
                .map(tp3 -> {
                    T next = isAlarmFirst() ? tp3.getT3() : tp3.getT2();
                    totalConsumer.accept(next, tp3.getT1());
                    return next;
                }), Integer.MAX_VALUE);
    }
}