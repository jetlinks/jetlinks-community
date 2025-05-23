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

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.ObjectUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Jia_RG , bestfeng ,zhouhao
 */
@Getter
@AllArgsConstructor
public enum ElasticSearch8xTermType implements ElasticSearchTermType {
    eq(TermType.eq) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {

            builder.term(termQuery -> termQuery
                .field(term.getColumn().trim())
                .value(FieldValue.of(term.getValue())));

            return builder;
        }
    },
    not(TermType.not) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.bool(bool -> bool
                .mustNot(mustNot -> mustNot
                    .term(termQuery -> termQuery
                        .field(term.getColumn().trim())
                        .value(FieldValue.of(term.getValue())))));
            return builder;
        }
    },
    isnull(TermType.isnull) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder
                .bool(bool -> bool
                    .mustNot(mustNot -> notnull.process(term, mustNot)));
            return builder;
        }

        @Override
        public Object convertTermValue(DataType type, Object value) {
            return value;
        }
    },
    notnull(TermType.notnull) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.exists(exists -> exists.field(term.getColumn()));
            return builder;
        }

        @Override
        public Object convertTermValue(DataType type, Object value) {
            return value;
        }
    },
    empty(TermType.empty) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.term(t -> t.field(term.getColumn().trim()).value(""));
            return builder;
        }

        @Override
        public Object convertTermValue(DataType type, Object value) {
            return value;
        }
    },
    nempty(TermType.nempty) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.bool(bool -> bool
                .mustNot(mustNot -> empty.process(term, mustNot)));
            return builder;
        }

        @Override
        public Object convertTermValue(DataType type, Object value) {
            return value;
        }
    },
    btw(TermType.btw) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            Number between = null;
            Number and = null;
            List<?> values = ConverterUtils.convertToList(term.getValue());
            int size = values.size();
            if (size > 0) {
                between = CastUtils.castNumber(values.get(0));
            }
            if (size > 1) {
                and = CastUtils.castNumber(values.get(1));
            }
            Number fb = between, ab = and;
            builder.range(range -> range
                .untyped(numberRange -> {
                    numberRange.gte(JsonData.of(fb));
                    numberRange.lte(JsonData.of(ab));
                    return numberRange.field(term.getColumn());
                }));
            return builder;
        }
    },
    gt(TermType.gt) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            Number value = CastUtils.castNumber(term.getValue());
            builder.range(range -> range
                .untyped(untyped -> untyped
                    .field(term.getColumn())
                    .gt(JsonData.of(value))));
            return builder;
        }
    },
    gte(TermType.gte) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            Number value = CastUtils.castNumber(term.getValue());
            builder.range(range -> range
                .untyped(untyped -> untyped
                    .field(term.getColumn())
                    .gte(JsonData.of(value))));
            return builder;
        }
    },
    lt(TermType.lt) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            Number value = CastUtils.castNumber(term.getValue());
            builder.range(range -> range
                .untyped(untyped -> untyped
                    .field(term.getColumn())
                    .lt(JsonData.of(value))));
            return builder;
        }
    },
    lte(TermType.lte) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            Number value = CastUtils.castNumber(term.getValue());
            builder.range(range -> range
                .untyped(untyped -> untyped
                    .field(term.getColumn())
                    .lte(JsonData.of(value))));
            return builder;
        }
    },
    in(TermType.in) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.terms(termQuery -> termQuery
                .field(term.getColumn().trim())
                .terms(t -> t.value(
                    ConverterUtils.convertToList(term.getValue(), FieldValue::of)
                )));
            return builder;
        }
    },
    nin(TermType.nin) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.bool(bool -> bool
                .mustNot(mustNot -> in.process(term, mustNot)));
            return builder;
        }
    },
    like(TermType.like) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.wildcard(wildcard -> wildcard
                .field(term.getColumn().trim())
                .value(likeQueryTermValueHandler(term.getValue())));
            return builder;
        }
    },
    nlike(TermType.nlike) {
        @Override
        public Query.Builder process(Term term, Query.Builder builder) {
            builder.bool(bool -> bool
                .mustNot(mustNot -> mustNot
                    .wildcard(wildcard -> wildcard
                        .field(term.getColumn().trim())
                        .value(likeQueryTermValueHandler(term.getValue())))));
            return builder;
        }
    };

    private final String type;

    @Override
    public boolean isSupported(Term term) {
        return this.type.equalsIgnoreCase(term.getTermType());
    }

    @Override
    public String getId() {
        return name();
    }

    private static String convertString(Object value) {
        if (value instanceof Collection) {
            return String.join(",", ((Collection<String>) value));
        } else {
            return String.valueOf(value);
        }
    }

    public static String likeQueryTermValueHandler(Object value) {
        if (!ObjectUtils.isEmpty(value)) {
            return convertString(value).replace("%", "*");
        }
        return "**";
    }

    public static Optional<ElasticSearch8xTermType> of(String type) {
        return Arrays.stream(values())
                     .filter(e -> e.getType().equalsIgnoreCase(type))
                     .findAny();
    }
}
