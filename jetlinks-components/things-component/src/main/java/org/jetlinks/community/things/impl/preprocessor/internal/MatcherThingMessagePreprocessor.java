package org.jetlinks.community.things.impl.preprocessor.internal;

import lombok.AllArgsConstructor;
import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.utils.ConverterUtils;
import org.jetlinks.community.things.preprocessor.ThingMessagePreprocessor;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@AllArgsConstructor
public abstract class MatcherThingMessagePreprocessor implements ThingMessagePreprocessor {

    protected final ThingMessagePreprocessorMatcher matcher;

    @Override
    public final Mono<ThingMessage> process(Context context) {
        return matcher
            .match(context)
            .flatMap(result -> {
                if (!result.isMatched()) {
                    return Mono.just(context.getMessage());
                }

                return process(result, context);
            });
    }

    protected abstract Mono<ThingMessage> process(ThingMessagePreprocessorMatcher.Result result, Context context);


    protected static abstract class Provider implements ThingMessagePreprocessor.Provider {

        protected abstract Mono<ThingMessagePreprocessor> create(ThingMessagePreprocessorMatcher matcher,
                                                                 Config config);

        @Override
        public Mono<ThingMessagePreprocessor> create(Config config) {

            @SuppressWarnings("all")
            Map<String, Object> matcher = config.get("matcher", Map.class).orElse(null);
            if (matcher == null) {
                return create(ThingMessagePreprocessorMatcher.alwaysMatched, config);
            }
            String provider = (String) matcher.get("provider");
            if (provider == null) {
                return Mono.error(new IllegalArgumentException("matcher.provider can not be null"));
            }
            Map<String, Object> configuration = ConverterUtils.convert(
                matcher.getOrDefault("configuration", Collections.emptyMap()),
                Map.class
            );

            Config conf = config.copy();
            conf.setConfiguration(configuration);

            return ThingMessagePreprocessorMatcher.Provider
                .supports
                .getNow(provider)
                .create(conf)
                .flatMap(_matcher -> create(_matcher, config));
        }
    }

}
