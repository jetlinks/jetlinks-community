package org.jetlinks.community.things.impl.preprocessor.internal.matchers;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.message.property.Property;
import org.jetlinks.core.things.Thing;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.community.things.impl.preprocessor.internal.ThingMessagePreprocessorMatcher;
import org.jetlinks.community.things.preprocessor.ThingMessagePreprocessor;
import org.jetlinks.reactor.ql.supports.filter.BetweenFilter;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 简单数字范围匹配
 * <pre>{@code
 *
 * {
 *    "provider":"number-range",
 *    "configuration":{
 *      "min":1,
 *      "max":10,
 *      "not":false
 *    }
 * }
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 2.2
 */
@AllArgsConstructor
public class NumberRangeMatcher implements ThingMessagePreprocessorMatcher {

    private final boolean not;
    private final Number max, min;

    @Override
    public Mono<Result> match(ThingMessagePreprocessor.Context context) {

        if (context.isWrapperFor(ThingMessagePreprocessor.PropertiesContext.class)) {
            Property property = context
                .unwrap(ThingMessagePreprocessor.PropertiesContext.class)
                .getProperty();

            boolean matched = BetweenFilter.predicate(property.getValue(), min, max);

            return createSpec(context.getThing(), property)
                .map(termSpecs -> new Result(not != matched, termSpecs));
        }

        //仅支持属性类型数据判断
        return Mono.empty();
    }

    private Mono<List<TermSpec>> createSpec(Thing thing, Property val) {

        return thing
            .getMetadata()
            .mapNotNull(metadata -> metadata
                .getProperty(val.getId())
                .orElse(null))
            .map(PropertyMetadata -> {
                List<TermSpec> termSpecs = new ArrayList<>();
                TermSpec spec = new TermSpec();
                spec.setColumn(PropertyMetadata.getId());
                spec.setDisplayName(PropertyMetadata.getName());
                spec.setType(Term.Type.or);
                spec.setTermType(not ? TermType.nbtw: TermType.btw);
                spec.setActual(val.getValue());
                spec.setExpected(Arrays.asList(min, max));
                termSpecs.add(spec);
                return termSpecs;
            })
            .defaultIfEmpty(new ArrayList<>());
    }


    public static class Provider implements ThingMessagePreprocessorMatcher.Provider {

        @Override
        public String getId() {
            return "number-range";
        }

        @Override
        public Mono<ThingMessagePreprocessorMatcher> create(ThingMessagePreprocessor.Config config) {
            return Mono.just(
                new NumberRangeMatcher(
                    config.getBoolean("not", false),
                    config.get("max").map(CastUtils::castNumber).orElse(0),
                    config.get("min").map(CastUtils::castNumber).orElse(Integer.MAX_VALUE)
                )
            );
        }
    }
}
