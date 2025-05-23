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
package org.jetlinks.community.rule.engine.scene;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.ShakeLimitResult;
import org.jetlinks.community.rule.engine.commons.impl.SimpleShakeLimitProvider;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.jetlinks.community.rule.engine.scene.SceneRule.SOURCE_ID_KEY;

/**
 * 场景触发支持提供商,用于提供对场景触发条件的支持.
 *
 * @param <E> E 配置类型
 * @author zhouhao
 * @since 2.1
 */
public interface SceneTriggerProvider<E extends SceneTriggerProvider.TriggerConfig> {

    /**
     * @return 提供商唯一标识
     */
    String getProvider();

    /**
     * @return 名称
     */
    String getName();

    /**
     * 创建配置
     *
     * @return 配置
     */
    E newConfig();

    /**
     * 根据配置以及条件创建SQL，该sql执行基于{@link org.jetlinks.reactor.ql.ReactorQL}.
     *
     * @param config    配置
     * @param terms     条件
     * @param hasFilter 是否包含过滤条件
     * @return SQL
     * @see org.jetlinks.reactor.ql.ReactorQL
     */
    SqlRequest createSql(E config, List<Term> terms, boolean hasFilter);

    /**
     * 订阅触发场景的原始数据流
     *
     * @param eventBus   事件总线
     * @param subscriber 订阅者标识
     * @param userId     用户ID
     * @param topic      topic
     * @return 数据流
     * @since 2.3
     */
    default Flux<Map<String, Object>> subscribe(EventBus eventBus,
                                                String subscriber,
                                                String userId,
                                                String topic) {
        return Flux.error(new UnsupportedOperationException("not support"));
    }


    /**
     * 判断当前服务是否支持此触发器
     *
     * @return 是否支持
     * @since 2.3
     */
    default Mono<Boolean> isSupported() {
        return Reactors.ALWAYS_TRUE;
    }

    /**
     * 处理SQL执行收到的数据,
     * 通过{@link SceneTriggerProvider#createSql(TriggerConfig, List, boolean)}和{@link EventBus}
     * 订阅到的数据将调用此方法处理.
     *
     * <pre>{@code
     *  select * from "/device/p1/d1/event/e1"
     * }</pre>
     *
     * @param payload 事件数据
     * @param output  处理结果接收器
     * @return void
     * @see SceneTriggerProvider#createSql(TriggerConfig, List, boolean)
     * @see EventBus#subscribe(Subscription, Function)
     * @since 2.3
     */
    default Mono<Void> handleSqlResult(TopicPayload payload,
                                       Consumer<Map<String, Object>> output) {
        return Mono.fromRunnable(() -> output.accept(payload.bodyToJson(true)));
    }

    /**
     * 创建过滤条件.不含where前缀.
     *
     * @param config 配置
     * @param terms  条件
     * @return 条件SQL语句
     * @see org.jetlinks.community.utils.ReactorUtils#createFilter(List)
     */
    SqlFragments createFilter(E config, List<Term> terms);

    /**
     * 重构条件为匹配场景输出变量的条件
     *
     * @param mainTableName 主表名
     * @param term          条件
     * @return 重构后的条件
     */
    default Term refactorTerm(String mainTableName,
                              Term term) {
        return SceneUtils.refactorTerm(mainTableName, term);
    }

    /**
     * 创建对用户友好的条件描述信息.
     *
     * @param config 配置信息
     * @param terms  条件
     * @return 描述信息
     * @since 2.2
     */
    default Mono<List<TermSpec>> createFilterSpec(E config, List<Term> terms, BiConsumer<Term, TermSpec> customizer) {
        return Mono.justOrEmpty(TermSpec.of(terms, customizer));
    }

    /**
     * 创建默认变量信息
     *
     * @param config 配置
     * @return 变量信息
     */
    List<Variable> createDefaultVariable(E config);

    default List<Variable> parseVariable(List<Term> terms,
                                         List<TermColumn> columns) {
        return SceneUtils.parseVariable(terms, columns);
    }

    /**
     * 应用规则节点配置
     *
     * @param config    触发器配置
     * @param model     规则模型
     * @param sceneNode 场景触发节点模型
     */
    void applyRuleNode(E config, RuleModel model, RuleNodeModel sceneNode);

    /**
     * 解析配置信息为支持的条件列,用于展示当前触发方式支持的触发条件.
     *
     * @param config 配置
     * @return 条件列信息
     */
    Flux<TermColumn> parseTermColumns(E config);

    /**
     * 防抖
     */
    default Flux<ShakeLimitResult<Map<String, Object>>> shakeLimit(String key,
                                                                   Flux<Map<String, Object>> source,
                                                                   ShakeLimit limit) {
        return shakeLimit(key,
                          source,
                          limit,
                          Mono.never());
    }


    default Flux<ShakeLimitResult<Map<String, Object>>> shakeLimit(String key,
                                                                   Flux<Map<String, Object>> source,
                                                                   ShakeLimit limit,
                                                                   Publisher<Map<String, Object>> resetSignal) {
        return SimpleShakeLimitProvider
            .GLOBAL
            .shakeLimit(
                key,
                source.groupBy(
                    data -> String.valueOf(data.getOrDefault(SOURCE_ID_KEY, "null")),
                    Integer.MAX_VALUE),
                limit,
                groupKey -> Flux
                    .from(resetSignal)
                    .filter(map -> Objects.equals(
                        String.valueOf(map.getOrDefault(SOURCE_ID_KEY, "null")),
                        groupKey
                    )));
    }

    interface TriggerConfig {
        void validate();

        default void with(Map<String, Object> config) {
            FastBeanCopier.copy(config, this);
        }
    }
}
