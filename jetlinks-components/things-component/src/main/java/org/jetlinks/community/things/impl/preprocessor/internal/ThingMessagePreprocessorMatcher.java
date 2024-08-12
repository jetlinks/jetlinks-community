package org.jetlinks.community.things.impl.preprocessor.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.community.things.preprocessor.ThingMessagePreprocessor;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * 物消息预处理匹配器,用于封装通用的预处理条件匹配逻辑
 *
 * @author zhouhao
 * @see Provider
 * @since 2.2
 */
public interface ThingMessagePreprocessorMatcher {


    Mono<Result> alwaysMatchedResult = Mono.just(new Result(true, Collections.emptyList()));
    Mono<Result> alwaysNoMatchedResult = Mono.just(new Result(false, Collections.emptyList()));

    ThingMessagePreprocessorMatcher alwaysMatched = (context) -> alwaysMatchedResult;
    ThingMessagePreprocessorMatcher alwaysNoMatched = (context) -> alwaysNoMatchedResult;

    /**
     * 执行匹配
     *
     * @param context 上下文
     * @return 匹配结果
     */
    Mono<Result> match(ThingMessagePreprocessor.Context context);

    interface Provider {

        org.jetlinks.community.spi.Provider<Provider> supports = org.jetlinks.community.spi.Provider.create(Provider.class);

        String getId();

        Mono<ThingMessagePreprocessorMatcher> create(ThingMessagePreprocessor.Config config);
    }


    @Getter
    @AllArgsConstructor
    class Result {
        private final boolean matched;

        private final List<TermSpec> spec;
    }


}