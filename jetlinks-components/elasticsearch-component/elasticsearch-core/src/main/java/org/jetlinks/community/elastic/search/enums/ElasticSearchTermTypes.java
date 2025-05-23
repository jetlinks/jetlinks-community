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
package org.jetlinks.community.elastic.search.enums;

import org.hswebframework.ezorm.core.param.Term;

import java.util.Optional;

public class ElasticSearchTermTypes {


    static {

    }

    public static void register(ElasticSearchTermType termType) {
        ElasticSearchTermType.supports.register(termType.getId(), termType);
    }

    public static ElasticSearchTermType lookupNow(Term term) {

        return lookup(term)
            .orElseThrow(() -> new UnsupportedOperationException("不支持的查询条件:" + term.getType()));
    }

    public static Optional<ElasticSearchTermType> lookup(Term term) {
        ElasticSearchTermType fast = ElasticSearchTermType
            .supports
            .get(term.getTermType())
            .orElse(null);
        if (fast != null) {
            return Optional.of(fast);
        }

        for (ElasticSearchTermType termType : ElasticSearchTermType.supports.getAll()) {
            if (termType.isSupported(term)) {
                return Optional.of(termType);
            }
        }
        return Optional.empty();
    }

}
