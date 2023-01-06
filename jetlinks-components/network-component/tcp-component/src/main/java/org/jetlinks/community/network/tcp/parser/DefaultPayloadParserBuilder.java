package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.tcp.parser.strateies.DelimitedPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.DirectPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.FixLengthPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.ScriptPayloadParserBuilder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class DefaultPayloadParserBuilder implements PayloadParserBuilder, BeanPostProcessor {

    private final Map<PayloadParserType, PayloadParserBuilderStrategy> strategyMap = new ConcurrentHashMap<>();

    public DefaultPayloadParserBuilder(){
        register(new FixLengthPayloadParserBuilder());
        register(new DelimitedPayloadParserBuilder());
        register(new ScriptPayloadParserBuilder());
        register(new DirectPayloadParserBuilder());
        register(new LengthFieldPayloadParserBuilder());
    }
    @Override
    public Supplier<PayloadParser> build(PayloadParserType type, ValueObject configuration) {

        return Optional.ofNullable(strategyMap.get(type))
                .map(builder -> builder.buildLazy(configuration))
                .orElseThrow(() -> new UnsupportedOperationException("unsupported parser:" + type));
    }

    public void register(PayloadParserBuilderStrategy strategy) {
        strategyMap.put(strategy.getType(), strategy);
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean,@Nonnull String beanName) throws BeansException {
        if (bean instanceof PayloadParserBuilderStrategy) {
            register(((PayloadParserBuilderStrategy) bean));
        }
        return bean;
    }
}
