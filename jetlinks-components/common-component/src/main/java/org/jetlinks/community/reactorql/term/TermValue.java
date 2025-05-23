/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.reactorql.term;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author zhangji 2025/2/27
 * @since 2.3
 */
@Getter
@Setter
public class TermValue implements Serializable {

    private static final long serialVersionUID = 1;

    @Schema(description = "来源")
    private Source source;

    @Schema(description = "[source]为[manual]时不能为空")
    private Object value;

    @Schema(description = "[source]为[metric]时不能为空")
    private String metric;

    @Schema(description = "[source]为[function]时不能为空")
    private String function;

    @Schema(description = "[source]为[function]时有效")
    private String column;

    @Schema(description = "[source]为[function]时有效")
    private Map<String, Object> args;

    public static TermValue manual(Object value) {
        TermValue termValue = new TermValue();
        termValue.setValue(value);
        termValue.setSource(Source.manual);
        return termValue;
    }

    public static TermValue metric(String metric) {
        TermValue termValue = new TermValue();
        termValue.setMetric(metric);
        termValue.setSource(Source.metric);
        return termValue;
    }

    public static List<TermValue> of(Term term) {
        return of(term.getValue());
    }

    public static List<TermValue> of(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Map) {
            return Collections.singletonList(FastBeanCopier.copy(value, new TermValue()));
        }
        if (value instanceof TermValue) {
            return Collections.singletonList(((TermValue) value));
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value)
                .stream()
                .flatMap(val -> of(val).stream())
                .collect(Collectors.toList());
        }
        return Collections.singletonList(TermValue.manual(value));
    }

    public enum Source {

        /**
         * 和manual一样,
         * 兼容{@link org.jetlinks.community.relation.utils.VariableSource.Source#fixed}
         */
        fixed,
        manual,

        metric,
        variable,
        /**
         * 和variable一样,兼容{@link org.jetlinks.community.relation.utils.VariableSource.Source#upper}
         */
        upper,

        /**
         * 函数
         *
         * @see org.jetlinks.community.reactorql.function.FunctionSupport
         */
        function
    }
}
