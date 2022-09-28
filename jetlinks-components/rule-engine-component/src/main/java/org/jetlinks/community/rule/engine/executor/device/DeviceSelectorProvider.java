package org.jetlinks.community.rule.engine.executor.device;

import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import org.springframework.core.Ordered;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface DeviceSelectorProvider extends Ordered {

    String getProvider();

    String getName();

    <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                       NestConditional<T> conditional);

    <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                       Map<String,Object> ctx,
                                                                       NestConditional<T> conditional);

    @Override
    default int getOrder() {
        return 0;
    }
}
