package org.jetlinks.community.rule.engine.executor.device;

import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.NestConditional;
import org.hswebframework.web.bean.FastBeanCopier;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class CompositeDeviceSelectorProvider implements DeviceSelectorProvider {

    public static final String PROVIDER = "composite";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return "组合选择";
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(List<?> args,
                                                                              NestConditional<T> conditional) {
        //暂不支持
        return Mono.just(conditional);
    }

    @Override
    public <T extends Conditional<T>> Mono<NestConditional<T>> applyCondition(DeviceSelectorSpec source,
                                                                              Map<String, Object> ctx,
                                                                              NestConditional<T> conditional) {
        Mono<NestConditional<T>> handler = null;
        for (SelectorValue selectorValue : source.getSelectorValues()) {
            DeviceSelectorSpec spec = FastBeanCopier.copy(selectorValue.getValue(), new DeviceSelectorSpec());
            if (handler == null) {
                handler = DeviceSelectorProviders
                    .getProviderNow(spec.getSelector())
                    .applyCondition(spec, ctx, conditional);
            } else {
                handler = handler
                    .flatMap(ctd -> DeviceSelectorProviders
                        .getProviderNow(spec.getSelector())
                        .applyCondition(spec, ctx, ctd));
            }
        }
        return handler == null ? Mono.just(conditional) : handler;
    }
}
