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
package org.jetlinks.community.rule.engine.scene.internal.triggers;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.EmptySqlRequest;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.EmptySqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.community.reactorql.term.FixedTermTypeSupport;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.rule.engine.scene.SceneTriggerProvider;
import org.jetlinks.community.rule.engine.scene.Variable;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.terms.I18nSpec;
import org.jetlinks.community.terms.TermSpec;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

import static org.jetlinks.community.rule.engine.scene.SceneRule.TRIGGER_TYPE;

@Component
public class TimerTriggerProvider implements SceneTriggerProvider<TimerTrigger> {
    public static final String PROVIDER = "timer";

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.scene_trigger_name_timer", "定时触发");
    }

    @Override
    public TimerTrigger newConfig() {
        return new TimerTrigger();
    }

    @Override
    public SqlRequest createSql(TimerTrigger config, List<Term> terms, boolean hasFilter) {
        return EmptySqlRequest.INSTANCE;
    }

    @Override
    public SqlFragments createFilter(TimerTrigger config, List<Term> terms) {
        return EmptySqlFragments.INSTANCE;
    }

    @Override
    public Mono<List<TermSpec>> createFilterSpec(TimerTrigger config,
                                                 List<Term> terms,
                                                 BiConsumer<Term, TermSpec> customizer) {
        TermSpec spec = new TermSpec();
        spec.setColumn(TRIGGER_TYPE);
        spec.setTermType(FixedTermTypeSupport.eq.name());
        spec.setTriggerSpec(
            I18nSpec.of("message.term_type_scene_timer_trigger_desc", config.toString())
        );
        spec.setActualSpec(
            I18nSpec.of("message.term_type_scene_timer_actual_desc", "定时触发告警")
        );
        spec.setDisplayCode(I18nSpec.of("message.scene_trigger_type", "场景触发类型"));
        spec.setExpected(PROVIDER);
        spec.setActual(PROVIDER);
        spec.setMatched(true);

        if (!terms.isEmpty()) {
            spec.setChildren(TermSpec.of(terms, customizer));
        }

        return Mono.just(Collections.singletonList(spec));
    }

    @Override
    public List<Variable> createDefaultVariable(TimerTrigger config) {
        return Collections.singletonList(
            Variable
                .of("_now",
                    LocaleUtils.resolveMessage(
                        "message.scene_term_column_now",
                        "服务器时间"))
                .withDescription(
                    LocaleUtils.resolveMessage(
                        "message.scene_term_column_now_desc",
                        "服务器时间"))
                .withType(DateTimeType.ID)
                .withTermType(TermTypes.lookup(DateTimeType.GLOBAL))
                .withColumn("_now")
                .withFullNameCode(I18nSpec.of("message.scene_term_column_now", "服务器时间"))
        );
    }

    @Override
    public Flux<TermColumn> parseTermColumns(TimerTrigger config) {
        return Flux.empty();
    }

    @Override
    public void applyRuleNode(TimerTrigger config, RuleModel model, RuleNodeModel sceneNode) {
        RuleNodeModel timerNode = new RuleNodeModel();
        timerNode.setId("scene:timer");
        timerNode.setName("定时触发场景");
        timerNode.setExecutor("timer");
        //使用最小负载节点来执行定时
        timerNode.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
        model.getNodes().add(timerNode);
        //定时->场景
        model.link(timerNode, sceneNode);
    }
}
