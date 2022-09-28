package org.jetlinks.community.rule.engine.executor.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
@SuppressWarnings("all")
public class SimpleDeviceSelectorProvider implements DeviceSelectorProvider {
    private final String provider;
    private final String name;

    private final BiFunction<List<?>, NestConditional<?>, Mono<NestConditional<?>>> function;

    public static SimpleDeviceSelectorProvider of(
        String provider,
        String name,
        BiFunction<List<?>, NestConditional<?>, NestConditional<?>> function) {
        return new SimpleDeviceSelectorProvider(provider, name, (args, condition) -> {
            return Mono.just(function.apply(args, condition));
        });
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                              NestConditional<T> conditional) {
        return (Mono) function.apply(args, conditional).defaultIfEmpty(conditional);
    }


    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                              Map<String, Object> ctx,
                                                                              NestConditional<T> conditional) {

        return source
            .resolveSelectorValues(ctx)
            .collectList()
            .flatMap(list -> applyCondition(list, conditional));
    }
}
